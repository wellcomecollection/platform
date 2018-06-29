package uk.ac.wellcome.platform.reindex_worker.services

import com.gu.scanamo.Scanamo
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.reindex_worker.TestRecord
import uk.ac.wellcome.platform.reindex_worker.fixtures.ReindexServiceFixture
import uk.ac.wellcome.platform.reindex_worker.models.{ReindexJob, ReindexRequest}
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
    with ReindexServiceFixture
    with SNS
    with SQS
    with ScalaFutures {

  def withReindexWorkerService(table: Table, topic: Topic)(
    testWith: TestWith[(ReindexWorkerService, QueuePair), Assertion]) = {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withLocalSqsQueueAndDlq {
          case queuePair @ QueuePair(queue, dlq) =>
            withSQSStream[NotificationMessage, Assertion](
              actorSystem,
              queue,
              metricsSender) { sqsStream =>
              withReindexService(table, topic) { reindexService =>
                val workerService = new ReindexWorkerService(
                  targetService = reindexService,
                  sqsStream = sqsStream,
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

  it("successfully completes a reindex") {
    withLocalDynamoDbTable { table =>
      withLocalSnsTopic { topic =>
        withReindexWorkerService(table, topic) {
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

            val expectedRecords = Seq(
              ReindexRequest(
                id = testRecord.id,
                desiredVersion = reindexJob.desiredVersion
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
              val actualRecords: Seq[ReindexRequest] = listMessagesReceivedFromSNS(topic)
                .map { _.message }
                .map { fromJson[ReindexRequest](_).get }
                .distinct

              actualRecords shouldBe expectedRecords
              assertQueueEmpty(queue)
              assertQueueEmpty(dlq)
            }
        }
      }
    }
  }

  it("fails if it cannot parse the SQS message as a ReindexJob") {
    withLocalDynamoDbTable { table =>
      withLocalSnsTopic { topic =>
        withReindexWorkerService(table, topic) {
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
  }

  it("fails if the reindex job fails") {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withLocalSqsQueueAndDlq {
          case queuePair @ QueuePair(queue, dlq) =>
            withSQSStream[NotificationMessage, Assertion](
              actorSystem,
              queue,
              metricsSender) { sqsStream =>
              val failingReindexService = mock[ReindexService]
              val targetService = mock[ReindexService]
              when(targetService.sendReindexRequests(any[ReindexJob]))
                .thenReturn(Future {
                  throw new RuntimeException(
                    "Flobberworm! Fickle failure frustrates my fortunes!")
                })

              new ReindexWorkerService(
                targetService = failingReindexService,
                system = actorSystem,
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
