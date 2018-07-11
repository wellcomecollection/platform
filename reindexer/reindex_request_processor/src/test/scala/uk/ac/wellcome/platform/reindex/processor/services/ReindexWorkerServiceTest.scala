package uk.ac.wellcome.platform.reindex.processor.services

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
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

class ReindexWorkerServiceTest extends FunSpec
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

  it("does not insert a new record if there is no existing record") {
    withLocalSqsQueueAndDlq { case queuePair @ QueuePair(queue, dlq) =>
      withLocalDynamoDbTable { table =>
        withReindexWorkerService(table, queue) { _ =>

          val reindexRequest = ReindexRequest("unknownId", 1)
          sendMessage(queue, reindexRequest)

          eventually {
            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, 1)
            assertNoRecords(table)
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
          givenRecord(SimpleReindexableRecord(id, recordVersion, reindexVersion, data), table)

          val updatedReindexVersion = 2
          sendMessage(queue, ReindexRequest(id, updatedReindexVersion))

          eventually {
            assertRecord(id,
              SimpleReindexableRecord(id, recordVersion+1, updatedReindexVersion, data), table)
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
          givenRecord(SimpleReindexableRecord(id, recordVersion, originalReindexVersion, data), table)

          val updatedReindexVersion = 1
          sendMessage(queue, ReindexRequest(id, updatedReindexVersion))

          eventually {
            assertQueueEmpty(queue)
            assertRecord(id,
              SimpleReindexableRecord(id, recordVersion, originalReindexVersion, data), table)
          }
        }
      }
    }
  }

  it("fails if saving to dynamo fails") {
    withLocalSqsQueueAndDlq { case queuePair @ QueuePair(queue, dlq) =>
      val badTable = Table("table","index")
      withReindexWorkerService(badTable, queue) { _ =>
        val reindexRequest = ReindexRequest("unknownId", 1)
        sendMessage(queue, reindexRequest)

        eventually {
          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, 1)
        }
      }
    }
  }

  it("updates a sourcedata record") {
    withLocalSqsQueue { queue =>
      withLocalDynamoDbTable { table =>
        withReindexWorkerService(table, queue) { _ =>

          case class SourceDataRecord(
                                       id: String,
                                       version: Int,
                                       reindexVersion: Int,
                                       reIndexShard: String,
                                       s3key: String,
                                       sourceName: String,
                                       sourceId: String)
          val sourceDataRecord = SourceDataRecord(
            id, 1, 100, "sierra/2058", "sierra/83/2371838/-324571730.json", "sierra", "L0054256")
          Scanamo.put(dynamoDbClient)(table.name)(sourceDataRecord)

          val updatedReindexVersion = 102
          sendMessage(queue, ReindexRequest(id, updatedReindexVersion))

          eventually {
            assertQueueEmpty(queue)
            val actualRecord = Scanamo.get[SourceDataRecord](dynamoDbClient)(table.name)('id -> id)
            actualRecord shouldBe Some(Right(sourceDataRecord.copy(version=2, reindexVersion=102)))
          }
        }
      }
    }
  }

  private def assertNoRecords(table: Table) = {
    val records = Scanamo.scan[ReindexableRecord](dynamoDbClient)(table.name)
    records.size shouldBe 0
  }

  private def assertRecord(id: String, record: SimpleReindexableRecord, table: Table) = {
    val actualRecord = Scanamo.get[SimpleReindexableRecord](dynamoDbClient)(table.name)('id -> id)
    actualRecord shouldBe Some(Right(record))
  }


  private def givenRecord(reindexableRecord: SimpleReindexableRecord, table: Table) = {
    Scanamo.put(dynamoDbClient)(table.name)(reindexableRecord)
  }

  private def sendMessage(queue: Queue, reindexRequest: ReindexRequest) = {
    sqsClient.sendMessage(queue.url,
      toJson(NotificationMessage("snsID", "snsTopic", "snsSubject", toJson(reindexRequest).get)).get)
  }

  private def withReindexWorkerService[R](table: Table,
                                          queue: Queue)(testWith: TestWith[ReindexWorkerService, R]) = {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withVersionedDao(table) { versionedDao =>
          withSQSStream[NotificationMessage, R](actorSystem, queue, metricsSender) { sqsStream =>

            val workerService = new ReindexWorkerService(versionedDao, sqsStream, actorSystem)

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
