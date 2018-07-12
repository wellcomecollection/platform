package uk.ac.wellcome.platform.reindex.processor.services

import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS.{Queue, QueuePair}
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}
import uk.ac.wellcome.models.reindexer.{ReindexRequest, ReindexableRecord}
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

class ReindexWorkerServiceTest
    extends FunSpec
    with Akka
    with LocalDynamoDbVersioned
    with SQS
    with ScalaFutures
    with Messaging
    with MetricsSenderFixture
    with ExtendedPatience {

  val id = "sierra/2371838"
  val data = "data"
  val recordVersion = 10

  case class SimpleReindexableRecord(id: String,
                                     version: Int,
                                     reindexVersion: Int,
                                     data: String)

  it("throws an error if the reindex request refers to a non-existent record") {
    withLocalSqsQueueAndDlq {
      case QueuePair(queue, dlq) =>
        withLocalDynamoDbTable { table =>
          withReindexWorkerService(table, queue) { _ =>
            val reindexRequest = ReindexRequest("unknownId", 1)
            sendNotificationToSQS(queue, reindexRequest)

            eventually {
              assertQueueEmpty(queue)
              assertQueueHasSize(dlq, 1)
              assertTableHasNoItems[ReindexableRecord](table)
            }
          }
        }
    }
  }

  it("updates an existing record with a newer version") {
    withLocalSqsQueue { queue =>
      withLocalDynamoDbTable { table =>
        withReindexWorkerService(table, queue) { _ =>
          val reindexVersion = 1
          val record =
            SimpleReindexableRecord(id, recordVersion, reindexVersion, data)
          givenTableHasItem(record, table)

          val updatedReindexVersion = 2
          sendNotificationToSQS(
            queue,
            ReindexRequest(id, updatedReindexVersion))

          eventually {
            assertTableOnlyHasItem(
              record.copy(
                version = record.version + 1,
                reindexVersion = updatedReindexVersion),
              table)
          }
        }
      }
    }
  }

  it("does not update an existing record with an older version") {
    withLocalSqsQueue { queue =>
      withLocalDynamoDbTable { table =>
        withReindexWorkerService(table, queue) { _ =>
          val originalReindexVersion = 2
          val record = SimpleReindexableRecord(
            id,
            recordVersion,
            originalReindexVersion,
            data)
          givenTableHasItem(record, table)

          val updatedReindexVersion = 1
          sendNotificationToSQS(
            queue,
            ReindexRequest(id, updatedReindexVersion))

          eventually {
            assertQueueEmpty(queue)
            assertTableOnlyHasItem(record, table)
          }
        }
      }
    }
  }

  it("fails if saving to dynamo fails") {
    withLocalSqsQueueAndDlq {
      case QueuePair(queue, dlq) =>
        val badTable = Table("table", "index")
        withReindexWorkerService(badTable, queue) { _ =>
          val reindexRequest = ReindexRequest("unknownId", 1)
          sendNotificationToSQS(queue, reindexRequest)

          eventually {
            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, 1)
          }
        }
    }
  }

  it(
    "updates a sourcedata record, preserving fields not involved in reindexing") {
    withLocalSqsQueue { queue =>
      withLocalDynamoDbTable { table =>
        withReindexWorkerService(table, queue) { _ =>
          case class SourceDataRecord(id: String,
                                      version: Int,
                                      reindexVersion: Int,
                                      reIndexShard: String,
                                      s3key: String,
                                      sourceName: String,
                                      sourceId: String)
          val sourceDataRecord = SourceDataRecord(
            id,
            1,
            100,
            "sierra/2058",
            "sierra/83/2371838/-324571730.json",
            "sierra",
            "L0054256")
          givenTableHasItem(sourceDataRecord, table)

          val updatedReindexVersion = 102
          sendNotificationToSQS(
            queue,
            ReindexRequest(id, updatedReindexVersion))

          eventually {
            assertQueueEmpty(queue)

            val actualrecord = getExistingTableItem[SourceDataRecord](id, table)
            actualrecord.version shouldBe sourceDataRecord.version + 1
            actualrecord.reindexVersion shouldBe updatedReindexVersion

            // Non reindexing field values are preserved
            actualrecord.reIndexShard shouldBe sourceDataRecord.reIndexShard
            actualrecord.s3key shouldBe sourceDataRecord.s3key
            actualrecord.sourceName shouldBe sourceDataRecord.sourceName
            actualrecord.sourceId shouldBe sourceDataRecord.sourceId
          }
        }
      }
    }
  }

  private def withReindexWorkerService[R](table: Table, queue: Queue)(
    testWith: TestWith[ReindexWorkerService, R]) = {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withVersionedDao(table) { versionedDao =>
          withSQSStream[NotificationMessage, R](
            actorSystem,
            queue,
            metricsSender) { sqsStream =>
            val workerService =
              new ReindexWorkerService(versionedDao, sqsStream, actorSystem)

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
}
