package uk.ac.wellcome.platform.reindex.creator

import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.models.reindexer.ReindexRequest
import uk.ac.wellcome.platform.reindex.creator.fixtures.ReindexFixtures
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.storage.vhs.HybridRecord

case class TestRecord(
  id: String,
  s3key: String,
  version: Int,
  reindexShard: String
)

class ReindexRequestCreatorFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with ExtendedPatience
    with fixtures.Server
    with LocalDynamoDbVersioned
    with ReindexFixtures
    with SNS
    with SQS
    with ScalaFutures {

  val shardName = "shard"

  private def createReindexableData(queue: Queue,
                                    table: Table): Seq[TestRecord] = {
    val numberOfRecords = 4

    val testRecords = (1 to numberOfRecords).map(i => {
      TestRecord(
        id = s"id$i",
        s3key = s"s3://$i",
        version = 1,
        reindexShard = shardName
      )
    })

    testRecords.map { testRecord =>
      Scanamo.put(dynamoDbClient)(table.name)(testRecord)
      testRecord
    }
  }

  it("sends a notification for every record that needs a reindex") {
    withLocalSqsQueue { queue =>
      withLocalDynamoDbTable { table =>
        withLocalSnsTopic { topic =>
          val flags = snsLocalFlags(topic) ++ dynamoClientLocalFlags ++ sqsLocalFlags(
            queue)

          withServer(flags) { _ =>
            val testRecords = createReindexableData(queue, table)

            val expectedRecords = testRecords.map { testRecord =>
              HybridRecord(
                id = testRecord.id,
                version = testRecord.version,
                s3key = testRecord.s3key
              )
            }

            val reindexJob = createReindexJobWith(
              table = table,
              shardId = shardName
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

              actualRecords should contain theSameElementsAs expectedRecords
            }
          }
        }
      }
    }
  }
}
