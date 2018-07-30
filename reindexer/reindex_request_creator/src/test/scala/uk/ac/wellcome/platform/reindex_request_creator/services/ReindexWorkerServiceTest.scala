package uk.ac.wellcome.platform.reindex_request_creator.services

import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.models.reindexer.ReindexRequest
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.reindex_request_creator.TestRecord
import uk.ac.wellcome.platform.reindex_request_creator.models.ReindexJob
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

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
              val readerService = new RecordReader(
                dynamoDbClient = dynamoDbClient,
                dynamoConfig = DynamoConfig(
                  table = table.name,
                  index = table.index
                )
              )

              withSNSWriter(topic) { snsWriter =>
                val notificationService = new NotificationSender(
                  snsWriter = snsWriter
                )

                val workerService = new ReindexWorkerService(
                  readerService = readerService,
                  notificationService = notificationService,
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

            sendNotificationToSQS(
              queue = queue,
              message = reindexJob
            )

            eventually {
              val actualRecords: Seq[ReindexRequest] =
                listMessagesReceivedFromSNS(topic)
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
            sendNotificationToSQS(
              queue = queue,
              body = "<xml>What is JSON.</xl?>"
            )

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
              val readerService = new RecordReader(
                dynamoDbClient = dynamoDbClient,
                dynamoConfig = DynamoConfig(
                  table = "doesnotexist",
                  index = "whatindex?"
                )
              )

              withSNSWriter(Topic("does-not-exist")) { snsWriter =>
                val notificationService = new NotificationSender(
                  snsWriter = snsWriter
                )

                new ReindexWorkerService(
                  readerService = readerService,
                  notificationService = notificationService,
                  system = actorSystem,
                  sqsStream = sqsStream
                )

                val reindexJob = ReindexJob(
                  shardId = "sierra/444",
                  desiredVersion = 4
                )

                sendNotificationToSQS(
                  queue = queue,
                  message = reindexJob
                )

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
