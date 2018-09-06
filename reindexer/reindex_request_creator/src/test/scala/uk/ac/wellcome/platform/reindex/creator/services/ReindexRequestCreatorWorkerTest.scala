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
import uk.ac.wellcome.platform.reindex.creator.fixtures.ReindexableTable
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.reindex.creator.dynamo.ParallelScanner
import uk.ac.wellcome.platform.reindex.creator.models.ReindexJob
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.vhs.HybridRecord

import scala.concurrent.ExecutionContext.Implicits.global

class ReindexRequestCreatorWorkerTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with Akka
    with ReindexableTable
    with MetricsSenderFixture
    with SNS
    with SQS
    with ScalaFutures {

  val exampleRecord = HybridRecord(
    id = "id",
    version = 1,
    location = ObjectLocation(
      namespace = "s3://example-bukkit",
      key = "key.json.gz"
    )
  )

  def withReindexWorkerService(table: Table, topic: Topic)(
    testWith: TestWith[(ReindexRequestCreatorWorker, QueuePair), Assertion]) = {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withLocalSqsQueueAndDlq {
          case queuePair @ QueuePair(queue, dlq) =>
            withSQSStream[NotificationMessage, Assertion](
              actorSystem,
              queue,
              metricsSender) { sqsStream =>
              val parallelScanner = new ParallelScanner(
                dynamoDBClient = dynamoDbClient,
                dynamoConfig = DynamoConfig(
                  table = table.name,
                  index = table.index
                )
              )

              val readerService = new RecordReader(
                parallelScanner = parallelScanner
              )

              withSNSWriter(topic) { snsWriter =>
                val hybridRecordSender = new HybridRecordSender(
                  snsWriter = snsWriter
                )

                val workerService = new ReindexRequestCreatorWorker(
                  readerService = readerService,
                  hybridRecordSender = hybridRecordSender,
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
        withReindexWorkerService(table, topic) {
          case (service, QueuePair(queue, dlq)) =>
            val reindexJob = ReindexJob(segment = 0, totalSegments = 1)

            Scanamo.put(dynamoDbClient)(table.name)(exampleRecord)

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

              actualRecords shouldBe List(exampleRecord)
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
    val badTable = Table(name = "doesnotexist", index = "whatindex")
    val badTopic = Topic("does-not-exist")

    withReindexWorkerService(badTable, badTopic) {
      case (_, QueuePair(queue, dlq)) =>
        val reindexJob = ReindexJob(segment = 5, totalSegments = 10)

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
