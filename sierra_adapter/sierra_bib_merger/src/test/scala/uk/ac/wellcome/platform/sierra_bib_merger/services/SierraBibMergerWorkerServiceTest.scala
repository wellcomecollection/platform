package uk.ac.wellcome.platform.sierra_bib_merger.services

import akka.actor.ActorSystem
import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import com.gu.scanamo.query.UniqueKey
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.SierraBibRecord
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.sierra_bib_merger.Server
import uk.ac.wellcome.platform.sierra_bib_merger.locals.DynamoDBLocal
import uk.ac.wellcome.models.MergedSierraRecord
import uk.ac.wellcome.models.SierraBibRecord._
import com.gu.scanamo.DynamoFormat
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, SQSLocal}
import uk.ac.wellcome.utils.JsonUtil


class SierraBibMergerWorkerServiceTest
    extends FunSpec
    with FeatureTestMixin
    with AmazonCloudWatchFlag
    with Matchers
    with SQSLocal
    with DynamoDBLocal {

  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher

  val queueUrl = createQueueAndReturnUrl("test_bib_merger")

  override protected def server = new EmbeddedHttpServer(
    new Server(),
    flags = Map(
      "aws.sqs.queue.url" -> queueUrl,
      "aws.sqs.waitTime" -> "1",
      "aws.dynamo.sierraBibMerger.tableName" -> tableName
    ) ++ sqsLocalFlags ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags
  )

  def bibRecordString(id: String,
                      updatedDate: String,
                      title: String = "Lehrbuch und Atlas der Gastroskopie") =
    s"""
      |{
      |      "id": "$id",
      |      "updatedDate": "$updatedDate",
      |      "createdDate": "1999-11-01T16:36:51Z",
      |      "deleted": false,
      |      "suppressed": false,
      |      "lang": {
      |        "code": "ger",
      |        "name": "German"
      |      },
      |      "title": "$title",
      |      "author": "Schindler, Rudolf, 1888-",
      |      "materialType": {
      |        "code": "a",
      |        "value": "Books"
      |      },
      |      "bibLevel": {
      |        "code": "m",
      |        "value": "MONOGRAPH"
      |      },
      |      "publishYear": 1923,
      |      "catalogDate": "1999-01-01",
      |      "country": {
      |        "code": "gw ",
      |        "name": "Germany"
      |      }
      |    }
    """.stripMargin

  it("should put a bib from SQS into DynamoDB") {
    val id = "1000001"
    val record = SierraBibRecord(
      id = id,
      data = bibRecordString(
        id = id,
        updatedDate = "2001-01-01T01:01:01Z",
        title = "One ocelot on our oval"
      ),
      modifiedDate = "2001-01-01T01:01:01Z"
    )
    sendBibRecordToSQS(record)
    val expectedMergedSierraRecord = MergedSierraRecord(bibRecord = record, version = 2)

    dynamoQueryEqualsValue('id -> id)(expectedValue = expectedMergedSierraRecord)
  }

  it("should put multiple bibs from SQS into DynamoDB") {
    val id1 = "1000001"
    val record1 = SierraBibRecord(
      id = id1,
      data = bibRecordString(
        id = id1,
        updatedDate = "2001-01-01T01:01:01Z",
        title = "The first ferret of four"
      ),
      modifiedDate = "2001-01-01T01:01:01Z"
    )
    sendBibRecordToSQS(record1)
    val expectedMergedSierraRecord1 = MergedSierraRecord(bibRecord = record1, version = 2)

    val id2 = "2000002"
    val record2 = SierraBibRecord(
      id = id2,
      data = bibRecordString(
        id = id2,
        updatedDate = "2002-02-02T02:02:02Z",
        title = "The second swan of a set"
      ),
      modifiedDate = "2002-02-02T02:02:02Z"
    )
    sendBibRecordToSQS(record2)
    val expectedMergedSierraRecord2 = MergedSierraRecord(bibRecord = record2, version = 2)

    dynamoQueryEqualsValue('id -> id1)(expectedValue = expectedMergedSierraRecord1)
    dynamoQueryEqualsValue('id -> id2)(expectedValue = expectedMergedSierraRecord2)
  }

  it("should update a bib in DynamoDB if a newer version is sent to SQS") {
    val id = "3000003"
    val oldBibRecord = SierraBibRecord(
      id = id,
      data = bibRecordString(
        id = id,
        updatedDate = "2003-03-03T03:03:03Z",
        title = "Old orangutans outside an office"
      ),
      modifiedDate = "2003-03-03T03:03:03Z"
    )
    val oldRecord = MergedSierraRecord(bibRecord = oldBibRecord)
    Scanamo.put(dynamoDbClient)(tableName)(oldRecord)

    val newTitle = "A number of new narwhals near Newmarket"
    val newUpdatedDate = "2004-04-04T04:04:04Z"
    val record = SierraBibRecord(
      id = id,
      data = bibRecordString(
        id = id,
        updatedDate = newUpdatedDate,
        title = newTitle
      ),
      modifiedDate = newUpdatedDate
    )
    println("@@AWLC Sent the second record to SQS")
    sendBibRecordToSQS(record)

    val expectedSierraRecord = MergedSierraRecord(bibRecord = record, version = 2)
    dynamoQueryEqualsValue('id -> id)(expectedValue = expectedSierraRecord)
  }

  it("should not update a bib in DynamoDB if an older version is sent to SQS") {
    val id = "6000006"
    val newBibRecord = SierraBibRecord(
      id = id,
      data = bibRecordString(
        id = id,
        updatedDate = "2006-06-06T06:06:06Z",
        title = "A presence of pristine porpoises"
      ),
      modifiedDate = "2006-06-06T06:06:06Z"
    )
    val newRecord = MergedSierraRecord(bibRecord = newBibRecord)
    Scanamo.put(dynamoDbClient)(tableName)(newRecord)
    val expectedSierraRecord = MergedSierraRecord(bibRecord = newBibRecord)

    val oldTitle = "A small selection of sad shellfish"
    val oldUpdatedDate = "2001-01-01T01:01:01Z"
    val record = SierraBibRecord(
      id = id,
      data = bibRecordString(
        id = id,
        updatedDate = oldUpdatedDate,
        title = oldTitle
      ),
      modifiedDate = oldUpdatedDate
    )
    sendBibRecordToSQS(record)

    // Blocking in Scala is generally a bad idea; we do it here so there's
    // enough time for this update to have gone through (if it was going to).
    Thread.sleep(5000)

    dynamoQueryEqualsValue('id -> id)(expectedValue = expectedSierraRecord)
  }

  it("should put a bib from SQS into DynamoDB if the ID exists but no bibData") {
    val id = "7000007"
    val newRecord = MergedSierraRecord(id = id)
    Scanamo.put(dynamoDbClient)(tableName)(newRecord)

    val title = "Inside an inquisitive igloo of ice imps"
    val updatedDate = "2007-07-07T07:07:07Z"
    val record = SierraBibRecord(
      id = id,
      data = bibRecordString(
        id = id,
        title = title,
        updatedDate = updatedDate
      ),
      modifiedDate = updatedDate
    )

    sendBibRecordToSQS(record)
    val expectedSierraRecord = MergedSierraRecord(bibRecord = record, version = 2)

    dynamoQueryEqualsValue('id -> id)(expectedValue = expectedSierraRecord)
  }

  private def sendBibRecordToSQS(record: SierraBibRecord) = {
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
  private def dynamoQueryEqualsValue[T: DynamoFormat](key: UniqueKey[_])(expectedValue: T) = {
    println(s"Searching DynamoDB for expectedValue = $expectedValue")
    eventually {
      val actualValue = Scanamo.get[T](dynamoDbClient)(tableName)(key).get
      actualValue shouldEqual Right(expectedValue)
    }
  }
}
