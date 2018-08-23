package uk.ac.wellcome.platform.reindex.creator.services

import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.reindex.creator.TestRecord
import uk.ac.wellcome.platform.reindex.creator.fixtures.{ReindexFixtures, ReindexableTable}
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.storage.vhs.HybridRecord

import scala.concurrent.ExecutionContext.Implicits.global

class ReindexRequestCreatorWorkerTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with Akka
    with ReindexableTable
    with MetricsSenderFixture
    with ReindexFixtures
    with SNS
    with SQS
    with ScalaFutures {

  def withReindexWorkerService(topic: Topic)(
    testWith: TestWith[(ReindexRequestCreatorWorker, QueuePair), Assertion]) = {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withLocalSqsQueueAndDlq {
          case queuePair @ QueuePair(queue, dlq) =>
            withSQSStream[NotificationMessage, Assertion](
              actorSystem,
              queue,
              metricsSender) { sqsStream =>
              val readerService = new RecordReader(
                dynamoDbClient = dynamoDbClient
              )

              withSNSWriter(topic) { snsWriter =>
                val notificationService = new NotificationSender(
                  snsWriter = snsWriter
                )

                val workerService = new ReindexRequestCreatorWorker(
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

  it("completes a reindex") {
    withLocalDynamoDbTable { table =>
      withLocalSnsTopic { topic =>
        withReindexWorkerService(topic) {
          case (service, QueuePair(queue, dlq)) =>
            val reindexJob = createReindexJobWith(
              table = table,
              shardId = "sierra/123"
            )

            val testRecord = TestRecord(
              id = "id/111",
              version = 1,
              s3key = "s3://id/111",
              reindexShard = reindexJob.shardId
            )

            Scanamo.put(dynamoDbClient)(table.name)(testRecord)

            val expectedRecords = List(
              HybridRecord(
                id = testRecord.id,
                version = testRecord.version,
                s3key = testRecord.s3key
              )
            )

            sendNotificationToSQS(
              queue = queue,
              message = reindexJob
            )

            eventually {
              val actualRecords: Seq[HybridRecord] =
                listMessagesReceivedFromSNS(topic)
                  .map { _.message }
                  .map { fromJson[HybridRecord](_).get }
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
        withReindexWorkerService(topic) {
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
                dynamoDbClient = dynamoDbClient
              )

              withSNSWriter(Topic("does-not-exist")) { snsWriter =>
                val notificationService = new NotificationSender(
                  snsWriter = snsWriter
                )

                new ReindexRequestCreatorWorker(
                  readerService = readerService,
                  notificationService = notificationService,
                  system = actorSystem,
                  sqsStream = sqsStream
                )

                val reindexJob = createReindexJobWith(
                  dynamoConfig = DynamoConfig(
                    table = "doesnotexist",
                    index = "whatindex?"
                  )
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
