package uk.ac.wellcome.platform.reindex_worker

import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.twitter.finatra.http.EmbeddedHttpServer
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.Matchers
import org.scalatest.FunSpec
import uk.ac.wellcome.locals.DynamoDBLocal
import uk.ac.wellcome.models.{Id, Versioned}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.reindex_worker.models.{
  CompletedReindexJob,
  ReindexJob,
  ReindexRecord
}
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.AmazonCloudWatchFlag
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._
import scala.collection.JavaConversions._

case class TestRecord(
  id: String,
  someData: String,
  version: Int,
  reindexShard: String,
  reindexVersion: Int
) extends Versioned
    with Id

class ReindexerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with DynamoDBLocal[TestRecord]
    with AmazonCloudWatchFlag
    with SqsFixtures
    with SnsFixtures
    with ScalaFutures {

  override lazy val evidence: DynamoFormat[TestRecord] =
    DynamoFormat[TestRecord]

  override lazy val tableName: String = "table"

  def withServer[R](queueUrl: String, topicArn: String)(
    testWith: TestWith[EmbeddedHttpServer, R]) = {
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

  def withBadServer[R](queueUrl: String, topicArn: String)(
    testWith: TestWith[EmbeddedHttpServer, R]) = {
    val server: EmbeddedHttpServer =
      new EmbeddedHttpServer(
        new Server(),
        flags = Map(
          "aws.dynamo.tableName" -> "not_a_real_table",
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

  val currentVersion = 1
  val desiredVersion = 5

  val shardName = "shard"

  private def createReindexableData(queueUrl: String): List[HybridRecord] = {
    val numberOfRecords = 4

    val testRecords = (1 to numberOfRecords).map(i => {
      TestRecord(
        id = s"id$i",
        version = 1,
        someData = "A ghastly gharial ganking a green golem.",
        reindexShard = shardName,
        reindexVersion = currentVersion
      )
    })

    //TODO re-factor shared test state here into fixture method
    testRecords.foreach(Scanamo.put(dynamoDbClient)(tableName)(_))

    val expectedRecords = testRecords.map(
      (r: TestRecord) =>
        ReindexRecord(
          id = r.id,
          version = r.version + 1,
          reindexShard = shardName,
          reindexVersion = desiredVersion
      ))

    val reindexJob = ReindexJob(
      shardId = shardName,
      desiredVersion = desiredVersion
    )

    val sqsMessage = SQSMessage(
      None,
      toJson(reindexJob).get,
      "topic",
      "message",
      "now"
    )

    sqsClient.sendMessage(queueUrl, toJson(sqsMessage).get)

    expectedRecords.toList
  }

  it("increases the reindexVersion on every record that needs a reindex") {
    withLocalSqsQueue { queueUrl =>
      withLocalSnsTopic { topicArn =>
        withServer(queueUrl, topicArn) { server =>
          sqsClient.setQueueAttributes(
            queueUrl,
            Map("VisibilityTimeout" -> "1"))

          val expectedRecords = createReindexableData(queueUrl)

          eventually {
            val actualRecords =
              Scanamo
                .scan[HybridRecord](dynamoDbClient)(tableName)
                .map(_.right.get)

            actualRecords should contain theSameElementsAs expectedRecords
          }

        }
      }
    }
  }

  it("sends an SNS notice for a completed reindex") {
    withLocalSqsQueue { queueUrl =>
      withLocalSnsTopic { topicArn =>
        withServer(queueUrl, topicArn) { server =>
          sqsClient.setQueueAttributes(
            queueUrl,
            Map("VisibilityTimeout" -> "1"))

          val expectedRecords = createReindexableData(queueUrl)

          val expectedMessage = CompletedReindexJob(
            shardId = shardName,
            completedReindexVersion = desiredVersion
          )

          eventually {

            val messages = listMessagesReceivedFromSNS(topicArn)

            messages should have size 1

            JsonUtil
              .fromJson[CompletedReindexJob](
                messages.head.message
              )
              .get shouldBe expectedMessage

          }
        }
      }
    }
  }

  it("does not send a message if it cannot complete a reindex") {
    withLocalSqsQueue { queueUrl =>
      withLocalSnsTopic { topicArn =>
        withBadServer(queueUrl, topicArn) { server =>
          sqsClient.setQueueAttributes(
            queueUrl,
            Map("VisibilityTimeout" -> "1"))

          val expectedRecords = createReindexableData(queueUrl)

          // We wait some time to ensure that the message is not processed
          Thread.sleep(5000)

          val messages = listMessagesReceivedFromSNS(topicArn)
          messages should have size 0
        }
      }
    }
  }
}
