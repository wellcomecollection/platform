package uk.ac.wellcome.platform.sierra_item_merger

import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{MergedSierraRecord, SierraItemRecord}
import uk.ac.wellcome.sierra_adapter.locals.DynamoDBLocal
import uk.ac.wellcome.platform.sierra_item_merger.utils.SierraItemMergerTestUtil
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, SQSLocal}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.dynamo._

class SierraItemMergerFeatureTest
    extends FunSpec
    with FeatureTestMixin
    with AmazonCloudWatchFlag
    with Matchers
    with SQSLocal
    with DynamoDBLocal
    with SierraItemMergerTestUtil
    with Eventually {

  val queueUrl = createQueueAndReturnUrl("test_item_merger")

  override protected def server = new EmbeddedHttpServer(
    new Server(),
    flags = Map(
      "aws.sqs.queue.url" -> queueUrl,
      "aws.sqs.waitTime" -> "1",
      "aws.dynamo.sierraItemMerger.tableName" -> tableName
    ) ++ sqsLocalFlags ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags
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

    eventually {
      dynamoQueryEqualsValue(id = bibId)(
        expectedValue = expectedMergedSierraRecord)
    }
  }

  it("should put multiple items from SQS into DynamoDB") {

    val bibId1 = "b1000001"

    val id1 = "1000001"
    val record1 = sierraItemRecord(
      id = id1,
      updatedDate = "2001-01-01T01:01:01Z",
      bibIds = List(bibId1)
    )
    sendItemRecordToSQS(record1)

    val bibId2 = "b2000002"
    val id2 = "2000002"
    val record2 = sierraItemRecord(
      id = id2,
      updatedDate = "2002-02-02T02:02:02Z",
      bibIds = List(bibId2)
    )
    sendItemRecordToSQS(record2)

    eventually {
      val expectedMergedSierraRecord1 = MergedSierraRecord(
        id = bibId1,
        itemData = Map(id1 -> record1),
        version = 1
      )
      val expectedMergedSierraRecord2 = MergedSierraRecord(
        id = bibId2,
        itemData = Map(id2 -> record2),
        version = 1
      )

      dynamoQueryEqualsValue(id = bibId1)(
        expectedValue = expectedMergedSierraRecord1)
      dynamoQueryEqualsValue(id = bibId2)(
        expectedValue = expectedMergedSierraRecord2)
    }
  }

  private def sendItemRecordToSQS(record: SierraItemRecord) = {
    val messageBody = JsonUtil.toJson(record).get

    val message = SQSMessage(
      subject = None,
      body = messageBody,
      topic = "topic",
      messageType = "messageType",
      timestamp = "2001-01-01T01:01:01Z"
    )
    sqsClient.sendMessage(queueUrl, JsonUtil.toJson(message).get)
  }
}
