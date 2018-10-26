package uk.ac.wellcome.platform.reindex.reindex_worker

import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.reindex.reindex_worker.models.ReindexJob
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.storage.vhs.HybridRecord

class ReindexWorkerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with IntegrationPatience
    with fixtures.Server
    with LocalDynamoDbVersioned
    with SNS
    with SQS
    with ScalaFutures {

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
          val flags = snsLocalFlags(topic) ++ dynamoDbLocalEndpointFlags(table) ++ sqsLocalFlags(
            queue)

          withServer(flags) { _ =>
            val testRecords = createReindexableData(table)

            val reindexJob = ReindexJob(segment = 0, totalSegments = 1) // , maxRecordsPerSegment = Some(1)

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

              actualRecords should contain theSameElementsAs testRecords
            }
          }
        }
      }
    }
  }

  it("sends the max number of notifications if option is set") {
    withLocalSqsQueue { queue =>
      withLocalDynamoDbTable { table =>
        withLocalSnsTopic { topic =>
          val flags = snsLocalFlags(topic) ++ dynamoDbLocalEndpointFlags(table) ++ sqsLocalFlags(
            queue)

          withServer(flags) { _ =>
            val testRecords = createReindexableData(table, 8)

            val reindexJob = ReindexJob(
              segment = 0,
              totalSegments = 1,
              maxRecordsPerSegment = Some(1))

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
