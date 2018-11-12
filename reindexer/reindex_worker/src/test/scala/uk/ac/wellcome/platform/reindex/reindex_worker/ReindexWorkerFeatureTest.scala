package uk.ac.wellcome.platform.reindex.reindex_worker

import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.reindex.reindex_worker.fixtures.WorkerServiceFixture
import uk.ac.wellcome.platform.reindex.reindex_worker.models.{CompleteReindexJob, PartialReindexJob, ReindexJob}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.storage.vhs.HybridRecord

class ReindexWorkerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with IntegrationPatience
    with LocalDynamoDbVersioned
    with SNS
    with SQS
    with ScalaFutures
    with WorkerServiceFixture {

  private def createReindexableData(
    table: Table,
    numberOfRecords: Int = 4): Seq[HybridRecord] = {
    val testRecords = (1 to numberOfRecords).map(i => {
      HybridRecord(
        id = s"id$i",
        location = ObjectLocation(
          namespace = "s3://example-bukkit",
          key = s"id$i"
        ),
        version = 1
      )
    })

    testRecords.foreach { testRecord =>
      Scanamo.put(dynamoDbClient)(table.name)(testRecord)
    }
    testRecords
  }

  it("sends a notification for every record that needs a reindex") {
    withLocalSqsQueue { queue =>
      withLocalDynamoDbTable { table =>
        withLocalSnsTopic { topic =>
          withWorkerService(queue, table, topic) { _ =>
            val testRecords = createReindexableData(table)

            val reindexJob = CompleteReindexJob(segment = 0, totalSegments = 1)

            sendNotificationToSQS[ReindexJob](
              queue = queue,
              message = reindexJob
            )

            eventually {
              val actualRecords: Seq[HybridRecord] =
                listMessagesReceivedFromSNS(topic)
                  .map { _.message }
                  .map { fromJson[HybridRecord](_).get }
                  .distinct

              actualRecords should contain theSameElementsAs testRecords
            }
          }
        }
      }
    }
  }

  it("can handle a partial reindex of the table") {
    withLocalSqsQueue { queue =>
      withLocalDynamoDbTable { table =>
        withLocalSnsTopic { topic =>
          withWorkerService(queue, table, topic) { _ =>
            val testRecords = createReindexableData(table, numberOfRecords = 8)

            val reindexJob = PartialReindexJob(maxRecords = 1)

            sendNotificationToSQS[ReindexJob](
              queue = queue,
              message = reindexJob
            )

            eventually {
              val actualRecords: Seq[HybridRecord] =
                listMessagesReceivedFromSNS(topic)
                  .map { _.message }
                  .map { fromJson[HybridRecord](_).get }
                  .distinct

              actualRecords should have length 1
              actualRecords should contain theSameElementsAs List(
                testRecords.head)
            }
          }
        }
      }
    }
  }
}
