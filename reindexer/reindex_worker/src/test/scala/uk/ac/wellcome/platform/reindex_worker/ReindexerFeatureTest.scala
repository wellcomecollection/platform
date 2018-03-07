package uk.ac.wellcome.platform.reindex_worker

import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.Matchers
import org.scalatest.fixture.FunSpec
import uk.ac.wellcome.locals.DynamoDBLocal
import uk.ac.wellcome.models.Sourced
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.reindex_worker.models.{CompletedReindexJob, ReindexJob}
import uk.ac.wellcome.storage.HybridRecord
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.{AmazonCloudWatchFlag, SNSLocal, SQSLocal}
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._
import scala.collection.JavaConversions._


class ReindexerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with DynamoDBLocal[HybridRecord]
    with AmazonCloudWatchFlag
    with SqsFixtures
    with SnsFixtures
    with ScalaFutures {

  override lazy val tableName: String = "table"

  override lazy val evidence: DynamoFormat[HybridRecord] =
    DynamoFormat[HybridRecord]

  private val enrichedDynamoFormat: DynamoFormat[HybridRecord] = Sourced
    .toSourcedDynamoFormatWrapper[HybridRecord]
    .enrichedDynamoFormat

  def withServer[R](queueUrl: String, topicArn: String)(testWith: TestWith[EmbeddedHttpServer, R]) = {
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

    server.start()

    try {
      testWith(server)
    } finally {
      server.close()
    }
  }

  override def withFixture(testWith: OneArgTest) = withLocalSqsQueue { queueUrl =>
    withLocalSnsTopic { topicArn =>
      withServer(queueUrl, topicArn) { server =>
        sqsClient.setQueueAttributes(queueUrl, Map("VisibilityTimeout" -> "1"))

        testWith(FixtureParam(queueUrl, topicArn))
      }
    }
  }

  case class FixtureParam(queueUrl: String, topicArn: String)

  val currentVersion = 1
  val desiredVersion = 5

  val shardName = "shard"

  private def createReindexableData(queueUrl: String): List[HybridRecord] = {
    val numberOfRecords = 4

    val hybridRecords = (1 to numberOfRecords).map(i => {
      HybridRecord(
        version = 1,
        sourceId = s"id$i",
        sourceName = "source",
        s3key = "s3://bucket/key",
        reindexShard = shardName,
        reindexVersion = currentVersion)
    })

    //TODO re-factor shared test state here into fixture method
    hybridRecords.foreach(
      Scanamo.put(dynamoDbClient)(tableName)(_)(enrichedDynamoFormat))

    val expectedRecords = hybridRecords.map(
      record =>
        record
          .copy(reindexVersion = desiredVersion, version = record.version + 1))

    val reindexJob = ReindexJob(
      shardId = shardName,
      desiredVersion = desiredVersion
    )

    val sqsMessage =
      SQSMessage(None, toJson(reindexJob).get, "topic", "message", "now")

    sqsClient.sendMessage(queueUrl, toJson(sqsMessage).get)

    expectedRecords.toList
  }

  it("increases the reindexVersion on every record that needs a reindex") { fixtures =>
    val expectedRecords = createReindexableData(fixtures.queueUrl)

    eventually {
      val actualRecords =
        Scanamo.scan[HybridRecord](dynamoDbClient)(tableName).map(_.right.get)

      actualRecords should contain theSameElementsAs expectedRecords
    }
  }

  it("sends an SNS notice for a completed reindex") { fixtures =>
    val expectedRecords = createReindexableData(fixtures.queueUrl)

    val expectedMessage = CompletedReindexJob(
      shardId = shardName,
      completedReindexVersion = desiredVersion
    )

    eventually {

      val messages = listMessagesReceivedFromSNS(fixtures.topicArn)

      messages should have size 1

      JsonUtil
        .fromJson[CompletedReindexJob](
          messages.head.message
        )
        .get shouldBe expectedMessage

    }
  }

  it("does not send a message if it cannot complete a reindex") { fixtures =>
    val expectedRecords = createReindexableData(fixtures.queueUrl)

    val badServer: EmbeddedHttpServer =
      new EmbeddedHttpServer(
        new Server(),
        flags = Map(
          "aws.dynamo.tableName" -> "not_a_real_table",
          "aws.region" -> "eu-west-1",
          "aws.sns.topic.arn" -> fixtures.topicArn,
          "aws.sqs.queue.url" -> fixtures.queueUrl
        ) ++ snsLocalFlags ++ cloudWatchLocalEndpointFlag ++ dynamoDbLocalEndpointFlags ++ sqsLocalFlags
      )

    badServer.start()

    // We wait some time to ensure that the message is not processed
    Thread.sleep(5000)

    val messages = listMessagesReceivedFromSNS(fixtures.topicArn)
    messages should have size 0

    badServer.close()
  }
}
