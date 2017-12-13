package uk.ac.wellcome.platform.sierra_item_merger

import akka.actor.ActorSystem
import com.gu.scanamo.query.UniqueKey
import com.gu.scanamo.syntax._
import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{MergedSierraRecord, SierraItemRecord}
import uk.ac.wellcome.platform.sierra_item_merger.locals.DynamoDBLocal
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, SQSLocal}
import uk.ac.wellcome.utils.JsonUtil

class SierraItemMergerFeatureTest
    extends FunSpec
    with FeatureTestMixin
    with AmazonCloudWatchFlag
    with Matchers
    with SQSLocal
    with DynamoDBLocal {

  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher

  val queueUrl = createQueueAndReturnUrl("test_item_merger")

  override protected def server = new EmbeddedHttpServer(
    new Server(),
    flags = Map(
      "aws.sqs.queue.url" -> queueUrl,
      "aws.sqs.waitTime" -> "1",
      "aws.dynamo.sierraItemMerger.tableName" -> tableName
    ) ++ sqsLocalFlags ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags
  )

  def itemRecordString(id: String,
                      updatedDate: String,
                      bibIds: List[String] = List()) =
    s"""
      |{
      |  "id": "$id",
      |  "updatedDate": "$updatedDate",
      |  "deleted": false,
      |  "bibIds": ${JsonUtil.toJson(bibIds).get},
      |  "fixedFields": {
      |    "85": {
      |      "label": "REVISIONS",
      |      "value": "1"
      |    },
      |    "86": {
      |      "label": "AGENCY",
      |      "value": "1"
      |    }
      |  },
      |  "varFields": [
      |    {
      |      "fieldTag": "c",
      |      "marcTag": "949",
      |      "ind1": " ",
      |      "ind2": " ",
      |      "subfields": [
      |        {
      |          "tag": "a",
      |          "content": "5722F"
      |        }
      |      ]
      |    }
      |  ]
      |}""".stripMargin

  private def sierraItemRecord(
    id: String,
    updatedDate: String,
    bibIds: List[String]
  ): SierraItemRecord =
    SierraItemRecord(
      id = id,
      data = itemRecordString(
        id = id,
        updatedDate = "2001-01-01T01:01:01Z",
        bibIds = bibIds
      ),
      bibIds = bibIds,
      modifiedDate = "2001-01-01T01:01:01Z"
    )

  it("should put an item from SQS into DynamoDB") {
    val id = "i1000001"
    val bibId = "b1000001"
    val record = sierraItemRecord(
      id = id,
      updatedDate = "2001-01-01T01:01:01Z",
      bibIds = List(bibId)
    )
    sendItemRecordToSQS(record)
    val expectedMergedSierraRecord = MergedSierraRecord(
      id = bibId,
      itemData = Map(id -> record),
      version = 1
    )

    dynamoQueryEqualsValue('id -> bibId)(
      expectedValue = expectedMergedSierraRecord)
  }

  // read unlinked IDs

  it("should put multiple items from SQS into DynamoDB") {

    val bibId1 = "b1000001"

    val id1 = "1000001"
    val record1 = sierraItemRecord(
      id = id1,
      updatedDate = "2001-01-01T01:01:01Z",
      bibIds = List(bibId1)
    )
    sendItemRecordToSQS(record1)
    val expectedMergedSierraRecord1 = MergedSierraRecord(
      id = bibId1,
      itemData = Map(id1 -> record1),
      version = 1
    )

    val bibId2 = "b2000002"
    val id2 = "2000002"
    val record2 = sierraItemRecord(
      id = id2,
      updatedDate = "2002-02-02T02:02:02Z",
      bibIds = List(bibId2)
    )
    sendItemRecordToSQS(record2)
    val expectedMergedSierraRecord2 = MergedSierraRecord(
      id = bibId2,
      itemData = Map(id2 -> record2),
      version = 1
    )

    dynamoQueryEqualsValue('id -> id1)(
      expectedValue = expectedMergedSierraRecord1)
    dynamoQueryEqualsValue('id -> id2)(
      expectedValue = expectedMergedSierraRecord2)
  }

  // put multiple items with same bibId

  it("should update an item in DynamoDB if a newer version is sent to SQS") {
    val id = "3000003"
    val bibId = "b3000003"
    val oldItemRecord = sierraItemRecord(
      id = id,
      updatedDate = "3003-03-03T03:03:03Z",
      bibIds = List(bibId)
    )
    val oldRecord = MergedSierraRecord(
      id = bibId,
      itemData = Map(id -> oldItemRecord),
      version = 1
    )
    Scanamo.put(dynamoDbClient)(tableName)(oldRecord)

    val newUpdatedDate = "2004-04-04T04:04:04Z"
    val newItemRecord = sierraItemRecord(
      id = id,
      updatedDate = newUpdatedDate,
      bibIds = List(bibId)
    )
    sendItemRecordToSQS(newItemRecord)

    val expectedSierraRecord = MergedSierraRecord(
      id = bibId,
      itemData = Map(id -> newItemRecord),
      version = 2
    )
    dynamoQueryEqualsValue('id -> id)(expectedValue = expectedSierraRecord)
  }

  // shouldn't unlink if older version

  it("should not update an item in DynamoDB if an older update is sent to SQS") {
    val id = "6000006"
    val bibId = "b6000006"
    val newItemRecord = sierraItemRecord(
      id = id,
      updatedDate = "2006-06-06T06:06:06Z",
      bibIds = List(bibId)
    )
    val newRecord = MergedSierraRecord(
      id = bibId,
      itemData = Map(id -> newItemRecord),
      version = 1
    )
    Scanamo.put(dynamoDbClient)(tableName)(newRecord)

    val oldItemRecord = sierraItemRecord(
      id = id,
      updatedDate = "2001-01-01T01:01:01Z",
      bibIds = List(bibId)
    )
    sendItemRecordToSQS(oldItemRecord)

    // Blocking in Scala is generally a bad idea; we do it here so there's
    // enough time for this update to have gone through (if it was going to).
    Thread.sleep(5000)

    dynamoQueryEqualsValue('id -> id)(expectedValue = newRecord)
  }

  it("should put an item from SQS into DynamoDB if the bibId exists but no itemData") {
    val bibId = "b7000007"
    val newRecord = MergedSierraRecord(id = bibId, version = 1)
    Scanamo.put(dynamoDbClient)(tableName)(newRecord)

    val itemRecord = sierraItemRecord(
      id = "i7000007",
      updatedDate = "2007-07-07T07:07:07Z",
      bibIds = List(bibId)
    )

    sendItemRecordToSQS(itemRecord)
    val expectedSierraRecord = MergedSierraRecord(
      id = bibId,
      itemData = Map(itemRecord.id -> itemRecord),
      version = 2
    )

    dynamoQueryEqualsValue('id -> bibId)(expectedValue = expectedSierraRecord)
  }

  private def sendItemRecordToSQS(record: SierraItemRecord) = {
    val messageBody = JsonUtil.toJson(record).get

    val message = SQSMessage(
      subject = Some("Test message sent by SierraBibMergerWorkerServiceTest"),
      body = messageBody,
      topic = "topic",
      messageType = "messageType",
      timestamp = "2001-01-01T01:01:01Z"
    )
    sqsClient.sendMessage(queueUrl, JsonUtil.toJson(message).get)
  }

  // TODO: This message is suitably generic, and could be moved
  // to DynamoDBLocal or another parent class, but requires some fiddling
  // with implicit ExecutionContexts to get right.  Move it!
  private def dynamoQueryEqualsValue[T: DynamoFormat](key: UniqueKey[_])(
    expectedValue: T) = {
    println(s"Searching DynamoDB for expectedValue = $expectedValue")
    eventually {
      val actualValue = Scanamo.get[T](dynamoDbClient)(tableName)(key).get
      actualValue shouldEqual Right(expectedValue)
    }
  }
}
