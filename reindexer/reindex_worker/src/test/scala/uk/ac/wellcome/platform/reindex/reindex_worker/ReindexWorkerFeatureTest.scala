package uk.ac.wellcome.platform.reindex.reindex_worker

import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.fixtures.{SNS, SQS}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.reindex.reindex_worker.fixtures.WorkerServiceFixture
import uk.ac.wellcome.platform.reindex.reindex_worker.models.{
  CompleteReindexParameters,
  PartialReindexParameters
}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.storage.vhs.HybridRecord

import scala.util.Random

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

  private def createHybridRecords: Seq[HybridRecord] =
    (1 to 4).map(i => {
      HybridRecord(
        id = s"id$i",
        location = ObjectLocation(
          namespace = "s3://example-bukkit",
          key = s"id$i"
        ),
        version = 1
      )
    })

  private def createReindexableData(table: Table): Seq[HybridRecord] = {
    val testRecords = createHybridRecords

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

            val reindexParameters =
              CompleteReindexParameters(segment = 0, totalSegments = 1)

            sendNotificationToSQS(
              queue = queue,
              message = createReindexRequestWith(parameters = reindexParameters)
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
            val testRecords = createReindexableData(table)

            val reindexParameters = PartialReindexParameters(maxRecords = 1)

            sendNotificationToSQS(
              queue = queue,
              message = createReindexRequestWith(parameters = reindexParameters)
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

  it("includes metadata for the rows") {
    case class Metadata(success: Boolean)

    case class CombinedRecord(
      id: String,
      location: ObjectLocation,
      version: Int,
      success: Boolean
    )

    val hybridRecords = createHybridRecords
    val metadataEntries = hybridRecords.map { hr =>
      Metadata(success = Random.nextFloat() < 0.5)
    }

    val recordsToIndex: Seq[CombinedRecord] =
      hybridRecords.zip(metadataEntries).map {
        case (hr: HybridRecord, m: Metadata) =>
          CombinedRecord(
            id = hr.id,
            location = hr.location,
            version = hr.version,
            success = m.success
          )
      }

    withLocalSqsQueue { queue =>
      withLocalDynamoDbTable { table =>
        recordsToIndex.foreach { record =>
          Scanamo.put(dynamoDbClient)(table.name)(record)
        }
        withLocalSnsTopic { topic =>
          withWorkerService(queue, table, topic) { _ =>
            val reindexParameters = CompleteReindexParameters(
              segment = 0,
              totalSegments = 1
            )

            sendNotificationToSQS(
              queue = queue,
              message = createReindexRequestWith(parameters = reindexParameters)
            )

            eventually {
              val messages: Seq[String] =
                listMessagesReceivedFromSNS(topic).map { _.message }.distinct

              val actualHybridRecords: Seq[HybridRecord] =
                messages.map { fromJson[HybridRecord](_).get }
              actualHybridRecords should contain theSameElementsAs hybridRecords

              val actualMetadataEntries: Seq[Metadata] =
                messages.map { fromJson[Metadata](_).get }
              actualMetadataEntries should contain theSameElementsAs metadataEntries
            }
          }
        }
      }
    }
  }
}
