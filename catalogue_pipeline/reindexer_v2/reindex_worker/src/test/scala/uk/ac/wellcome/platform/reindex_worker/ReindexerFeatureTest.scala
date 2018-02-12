package uk.ac.wellcome.platform.reindex_worker

import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.locals.DynamoDBLocal
import uk.ac.wellcome.models.Versioned
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.reindex_worker.models.{
  CompletedReindexJob,
  ReindexJob
}
import uk.ac.wellcome.storage.HybridRecord
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, SNSLocal, SQSLocal}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

class ReindexerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with DynamoDBLocal[HybridRecord]
    with AmazonCloudWatchFlag
    with SQSLocal
    with SNSLocal
    with ScalaFutures {

  override lazy val tableName: String = "table"

  override lazy val evidence: DynamoFormat[HybridRecord] =
    DynamoFormat[HybridRecord]

  private val enrichedDynamoFormat: DynamoFormat[HybridRecord] = Versioned
    .toVersionedDynamoFormatWrapper[HybridRecord]
    .enrichedDynamoFormat

  val queueUrl = createQueueAndReturnUrl("reindexer-feature-test-q")
  val topicArn = createTopicAndReturnArn("test_reindexer")

  val server: EmbeddedHttpServer =
    new EmbeddedHttpServer(
      new Server(),
      flags = Map(
        "aws.dynamo.tableName" -> tableName,
        "aws.region" -> "eu-west-1",
        "aws.sns.topic.arn" -> topicArn,
        "aws.sqs.queue.url" -> queueUrl
      ) ++ snsLocalFlags ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags ++ sqsLocalFlags
    )

  val currentVersion = 1
  val desiredVersion = 5

  val shardName = "shard"

  private def createReindexableData: List[HybridRecord] = {
    val numberOfRecords = 4

    val hybridRecords = (1 to numberOfRecords).map(i => {
      HybridRecord(version = 1,
                   sourceId = s"id$i",
                   sourceName = "source",
                   s3key = "s3://bucket/key",
                   reindexShard = shardName,
                   reindexVersion = currentVersion)
    })

    hybridRecords.foreach(
      Scanamo.put(dynamoDbClient)(tableName)(_)(enrichedDynamoFormat))

    val expectedRecords = hybridRecords.map(
      record =>
        record.copy(reindexVersion = desiredVersion,
                    version = record.version + 1))

    val reindexJob = ReindexJob(
      shardId = shardName,
      desiredVersion = desiredVersion
    )

    val sqsMessage =
      SQSMessage(None, toJson(reindexJob).get, "topic", "message", "now")
    sqsClient.sendMessage(queueUrl, toJson(sqsMessage).get)

    expectedRecords.toList
  }

  it("increases the reindexVersion on every record that needs a reindex") {
    val expectedRecords = createReindexableData

    server.start()

    eventually {
      val actualRecords =
        Scanamo.scan[HybridRecord](dynamoDbClient)(tableName).map(_.right.get)

      actualRecords should contain theSameElementsAs expectedRecords
    }

    server.close()

  }

  it("sends an SNS notice for a completed reindex") {
    val expectedRecords = createReindexableData

    server.start()

    val expectedMessage = CompletedReindexJob(
      shardId = shardName,
      completedReindexVersion = desiredVersion
    )

    eventually {

      val messages = listMessagesReceivedFromSNS()

      messages should have size 1

      JsonUtil
        .fromJson[CompletedReindexJob](
          messages.head.message
        )
        .get shouldBe expectedMessage

    }

    server.close()
  }

  it("does not send a message if it cannot complete a reindex") {
    val expectedRecords = createReindexableData

    val badServer: EmbeddedHttpServer =
      new EmbeddedHttpServer(
        new Server(),
        flags = Map(
          "aws.dynamo.tableName" -> "not_a_real_table",
          "aws.region" -> "eu-west-1",
          "aws.sns.topic.arn" -> topicArn,
          "aws.sqs.queue.url" -> queueUrl
        ) ++ snsLocalFlags ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags ++ sqsLocalFlags
      )

    badServer.start()

    // We wait some time to ensure that the message is not processed
    Thread.sleep(5000)

    val messages = listMessagesReceivedFromSNS()
    messages should have size 0

    badServer.close()
  }
}
