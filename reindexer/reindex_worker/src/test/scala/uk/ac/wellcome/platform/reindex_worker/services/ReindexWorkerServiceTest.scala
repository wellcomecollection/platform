package uk.ac.wellcome.platform.reindex_worker.services

import com.gu.scanamo.Scanamo
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig, SNSWriter}
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.reindex_worker.TestRecord
import uk.ac.wellcome.platform.reindex_worker.models.ReindexJob
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReindexWorkerServiceTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with Akka
    with LocalDynamoDbVersioned
    with MetricsSenderFixture
    with SNS
    with SQS
    with ScalaFutures {

  def withReindexWorkerService(table: Table)(
    testWith: TestWith[(ReindexWorkerService, QueuePair), Assertion]) = {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withLocalSqsQueueAndDlq {
          case queuePair @ QueuePair(queue, dlq) =>
            withLocalSnsTopic { topic =>
              withSQSStream[NotificationMessage, Assertion](
                actorSystem,
                queue,
                metricsSender) { sqsStream =>
                withReindexService(table) { reindexService =>
                  val workerService = new ReindexWorkerService(
                    targetService = reindexService,
                    sqsStream = sqsStream,
                    snsWriter = new SNSWriter(
                      snsClient = snsClient,
                      snsConfig = SNSConfig(topicArn = topic.arn)
                    ),
                    system = actorSystem
                  )

                  try {
                    testWith((workerService, queuePair))
                  } finally {
                    workerService.stop()
                  }
                }
              }
            }
        }
      }
    }
  }

  def withReindexService[R](table: Table)(
    testWith: TestWith[ReindexService, R]) = {
    val reindexService = new ReindexService(
      dynamoDbClient = dynamoDbClient,
      dynamoConfig = DynamoConfig(table = table.name, index = table.index)
    )
    testWith(reindexService)
  }

  it("successfully completes a reindex") {
    withLocalDynamoDbTable { table =>
      withReindexWorkerService(table) {
        case (service, QueuePair(queue, dlq)) =>
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

          Scanamo.put(dynamoDbClient)(table.name)(testRecord)

          val expectedRecords = List(
            testRecord.copy(
              version = testRecord.version + 1,
              reindexVersion = reindexJob.desiredVersion
            )
          )

          val sqsMessage = NotificationMessage(
            Subject = "",
            Message = toJson(reindexJob).get,
            TopicArn = "topic",
            MessageId = "message"
          )

          sqsClient.sendMessage(queue.url, toJson(sqsMessage).get)

          eventually {
            val actualRecords: List[TestRecord] =
              Scanamo
                .scan[TestRecord](dynamoDbClient)(table.name)
                .map(_.right.get)

            actualRecords shouldBe expectedRecords
            assertQueueEmpty(queue)
            assertQueueEmpty(dlq)
          }
      }
    }
  }

  it("fails if it cannot parse the SQS message as a ReindexJob") {
    withLocalDynamoDbTable { table =>
      withReindexWorkerService(table) {
        case (_, QueuePair(queue, dlq)) =>
          val sqsMessage = NotificationMessage(
            Subject = "",
            Message = "<xml>What is JSON.</xl?>",
            TopicArn = "topic",
            MessageId = "message-id"
          )

          sqsClient.sendMessage(queue.url, toJson(sqsMessage).get)

          eventually {
            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, 1)
          }
      }
    }
  }

  it("fails if the reindex job fails") {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withLocalSnsTopic { topic =>
          withLocalSqsQueueAndDlq {
            case queuePair @ QueuePair(queue, dlq) =>
              withSQSStream[NotificationMessage, Assertion](
                actorSystem,
                queue,
                metricsSender) { sqsStream =>
                val failingReindexService = mock[ReindexService]
                val targetService = mock[ReindexService]
                when(targetService.runReindex(any[ReindexJob]))
                  .thenReturn(Future {
                    throw new RuntimeException(
                      "Flobberworm! Fickle failure frustrates my fortunes!")
                  })

                new ReindexWorkerService(
                  targetService = failingReindexService,
                  system = actorSystem,
                  snsWriter = new SNSWriter(
                    snsClient = snsClient,
                    snsConfig = SNSConfig(topicArn = topic.arn)
                  ),
                  sqsStream = sqsStream
                )

                val reindexJob = ReindexJob(
                  shardId = "sierra/444",
                  desiredVersion = 4
                )

                val sqsMessage = NotificationMessage(
                  Subject = "",
                  Message = toJson(reindexJob).get,
                  TopicArn = "topic",
                  MessageId = "message"
                )

                sqsClient.sendMessage(queue.url, toJson(sqsMessage).get)

                eventually {
                  assertQueueEmpty(queue)
                  assertQueueHasSize(dlq, 1)
                }
              }
          }
        }
      }
    }
  }
}
