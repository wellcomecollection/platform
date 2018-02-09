package uk.ac.wellcome.platform.sierra_items_to_dynamo

import java.time.Instant

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.server.FeatureTestMixin
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.sierra_items_to_dynamo.locals.SierraItemsToDynamoDBLocal
import uk.ac.wellcome.test.utils.{
  AmazonCloudWatchFlag,
  ExtendedPatience,
  SQSLocal
}
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.models.transformable.sierra.{
  SierraItemRecord,
  SierraRecord
}

class SierraItemsToDynamoFeatureTest
    extends FunSpec
    with FeatureTestMixin
    with SQSLocal
    with SierraItemsToDynamoDBLocal
    with AmazonCloudWatchFlag
    with Matchers
    with ExtendedPatience {
  val queueUrl = createQueueAndReturnUrl("sierra-test-queue")

  override protected def server = new EmbeddedHttpServer(
    new Server(),
    Map(
      "aws.sqs.queue.url" -> queueUrl,
      "aws.sqs.waitTime" -> "1",
      "aws.dynamo.tableName" -> tableName
    ) ++ sqsLocalFlags ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags
  )

  it("reads items from Sierra and adds them to DynamoDB") {
    val id = "i12345"
    val bibId = "b54321"
    val data = s"""{"id": "$id", "bibIds": ["$bibId"]}"""
    val modifiedDate = Instant.ofEpochSecond(Instant.now.getEpochSecond)
    val message = SierraRecord(id, data, modifiedDate)

    val sqsMessage =
      SQSMessage(Some("subject"),
                 toJson(message).get,
                 "topic",
                 "messageType",
                 "timestamp")
    sqsClient.sendMessage(queueUrl, toJson(sqsMessage).get)

    eventually {
      Scanamo.scan[SierraItemRecord](dynamoDbClient)(tableName) should have size 1
      val scanamoResult =
        Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)('id -> id)
      scanamoResult shouldBe defined
      scanamoResult.get shouldBe Right(
        SierraItemRecord(id, data, modifiedDate, List(bibId), version = 1))
    }
  }
}
