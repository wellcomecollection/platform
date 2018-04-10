package uk.ac.wellcome.platform.reindex_worker.services

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.gu.scanamo.{DynamoFormat, Scanamo}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.dynamo.VersionedDao
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{
  DynamoConfig,
  SNSConfig,
  SQSConfig,
  SQSMessage
}
import uk.ac.wellcome.platform.reindex_worker.TestRecord
import uk.ac.wellcome.platform.reindex_worker.models.ReindexJob
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.SQSReader
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class ReindexWorkerServiceTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with Akka
    with LocalDynamoDb[TestRecord]
    with SNS
    with SQS
    with ScalaFutures {

  override lazy val evidence: DynamoFormat[TestRecord] =
    DynamoFormat[TestRecord]

  def withReindexWorkerService(tableName: String, indexName: String)(
    testWith: TestWith[ReindexWorkerService, Assertion]) = {
    withActorSystem { actorSystem =>
      val metricsSender = new MetricsSender(
        namespace = "reindex-worker-service-test",
        flushInterval = 100 milliseconds,
        amazonCloudWatch = mock[AmazonCloudWatch],
        actorSystem = actorSystem
      )

      withLocalSqsQueue { queueUrl =>
        withLocalSnsTopic { topic =>
          val workerService = new ReindexWorkerService(
            targetService = new ReindexService(
              dynamoDBClient = dynamoDbClient,
              metricsSender = metricsSender,
              versionedDao = new VersionedDao(
                dynamoDbClient = dynamoDbClient,
                dynamoConfig = DynamoConfig(table = tableName)
              ),
              dynamoConfig = DynamoConfig(table = tableName),
              indexName = indexName
            ),
            reader = new SQSReader(
              sqsClient = sqsClient,
              sqsConfig = SQSConfig(
                queueUrl = queueUrl,
                waitTime = 1 second,
                maxMessages = 1
              )
            ),
            snsWriter = new SNSWriter(
              snsClient = snsClient,
              snsConfig = SNSConfig(topicArn = topic.arn)
            ),
            system = actorSystem,
            metrics = metricsSender
          )

          try {
            testWith(workerService)
          } finally {
            workerService.stop()
          }
        }
      }
    }
  }

  it("returns a successful Future if the reindex completes correctly") {
    withLocalDynamoDbTableAndIndex { fixtures =>
      val tableName = fixtures.tableName
      val indexName = fixtures.indexName
      withReindexWorkerService(tableName, indexName) { service =>
        val reindexJob = ReindexJob(
          shardId = "sierra/123",
          desiredVersion = 6
        )

        val testRecord = TestRecord(
          id = "id/111",
          version = 1,
          someData = "A dire daliance directly dancing due down.",
          reindexShard = reindexJob.shardId,
          reindexVersion = reindexJob.desiredVersion - 1
        )

        Scanamo.put(dynamoDbClient)(tableName)(testRecord)

        val expectedRecords = List(
          testRecord.copy(
            version = testRecord.version + 1,
            reindexVersion = reindexJob.desiredVersion
          )
        )

        val sqsMessage = SQSMessage(
          subject = None,
          body = toJson(reindexJob).get,
          topic = "topic",
          messageType = "message",
          timestamp = "now"
        )

        val future = service.processMessage(message = sqsMessage)

        whenReady(future) { _ =>
          val actualRecords: List[TestRecord] =
            Scanamo
              .scan[TestRecord](dynamoDbClient)(tableName)
              .map(_.right.get)

          actualRecords shouldBe expectedRecords
        }
      }
    }
  }

  it(
    "returns a failed Future if it cannot parse the SQS message as a ReindexJob") {
    withLocalDynamoDbTableAndIndex { fixtures =>
      val tableName = fixtures.tableName
      val indexName = fixtures.indexName
      withReindexWorkerService(tableName, indexName) { service =>
        val sqsMessage = SQSMessage(
          subject = None,
          body = "<xml>What is JSON.</xl?>",
          topic = "topic",
          messageType = "message",
          timestamp = "now"
        )

        val future = service.processMessage(message = sqsMessage)

        whenReady(future.failed) { result =>
          result shouldBe a[GracefulFailureException]
          result.getMessage should include(
            "expected json value got < (line 1, column 1)")
        }
      }
    }
  }

  it("returns a failed Future if the reindex job fails") {
    withActorSystem { actorSystem =>
      val metricsSender = new MetricsSender(
        namespace = "reindex-worker-service-test",
        flushInterval = 100 milliseconds,
        amazonCloudWatch = mock[AmazonCloudWatch],
        actorSystem = actorSystem
      )

      val exception = new RuntimeException(
        "Flobberworm! Fickle failure frustrates my fortunes!")

      val targetService = mock[ReindexService]
      when(targetService.runReindex(any[ReindexJob]))
        .thenReturn(Future { throw exception })

      val service = new ReindexWorkerService(
        targetService = targetService,
        reader = mock[SQSReader],
        snsWriter = mock[SNSWriter],
        system = actorSystem,
        metrics = metricsSender
      )

      val reindexJob = ReindexJob(
        shardId = "sierra/444",
        desiredVersion = 4
      )

      val sqsMessage = SQSMessage(
        subject = None,
        body = toJson(reindexJob).get,
        topic = "topic",
        messageType = "message",
        timestamp = "now"
      )

      val future = service.processMessage(message = sqsMessage)

      whenReady(future.failed) { _ shouldBe exception }
    }
  }
}
