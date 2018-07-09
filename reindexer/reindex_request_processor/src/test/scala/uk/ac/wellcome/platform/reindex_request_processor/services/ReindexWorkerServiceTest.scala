package uk.ac.wellcome.platform.reindex_request_processor.services

import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSStream}
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}
import uk.ac.wellcome.models.reindexer.{ReindexRequest, ReindexableRecord}
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.EmptyMetadata
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class ReindexWorkerServiceTest extends FunSpec
  with Akka
  with LocalVersionedHybridStore
  with SQS
  with ScalaFutures
  with Messaging
  with MetricsSenderFixture
  with ExtendedPatience {

  it("creates a new record") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { vhsBucket =>
          withLocalDynamoDbTable { vhsTable =>
          withReindexWorkerService(vhsTable, vhsBucket, queue) { _ =>

            val version = 1
            val id = "abc123"
            val reindexRequest = ReindexRequest(id = id, desiredVersion = version)

            sendMessage(queue, reindexRequest)

            eventually {
              assertStored[ReindexableRecord](vhsBucket, vhsTable,
                ReindexableRecord(id, version))
            }
          }
        }
      }
    }
  }

  it("updates an existing record with a newer version") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { vhsBucket =>
        withLocalDynamoDbTable { vhsTable =>
          withReindexWorkerService(vhsTable, vhsBucket, queue) { _ =>

            val id = "abc123"
            val originalVersion = 1
            val updatedVersion = 2

            val reindexRequest = ReindexRequest(id = id, desiredVersion = originalVersion)
            sendMessage(queue, reindexRequest)
            sendMessage(queue, reindexRequest.copy(desiredVersion = updatedVersion))

            eventually {
              assertStored[ReindexableRecord](vhsBucket, vhsTable,
                ReindexableRecord(id, updatedVersion))
            }
          }
        }
      }
    }
  }

  private def sendMessage(queue: Queue, reindexRequest: ReindexRequest) = {
    sqsClient.sendMessage(queue.url,
      toJson(NotificationMessage("snsID", "snsTopic", "snsSubject", toJson(reindexRequest).get)).get)
  }

  private def withReindexWorkerService[R](vhsTable: Table,
                                          vhsBucket: Bucket,
                                          queue: Queue)(testWith: TestWith[ReindexWorkerService, R]) = {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withTypeVHS[ReindexableRecord, EmptyMetadata, R](
          bucket = vhsBucket,
          table = vhsTable) { versionedHybridStore =>

          val sqsStream = new SQSStream[NotificationMessage](
            actorSystem,
            asyncSqsClient,
            SQSConfig(queue.url, waitTime = 1 second, maxMessages = 1),
            metricsSender)

          val workerService = new ReindexWorkerService(
              versionedHybridStore = versionedHybridStore,
              sqsStream = sqsStream,
              system = actorSystem
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

}
