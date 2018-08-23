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

case class TestRecord(
  id: String,
  s3key: String,
  version: Int,
  reindexShard: String,
  reindexVersion: Int
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

  val currentVersion = 1
  val desiredVersion = 5

  val shardName = "shard"

  private def createReindexableData(queue: Queue,
                                    table: Table): Seq[ReindexRequest] = {
    val numberOfRecords = 4

    val testRecords = (1 to numberOfRecords).map(i => {
      TestRecord(
        id = s"id$i",
        s3key = s"s3://$i",
        version = 1,
        reindexShard = shardName,
        reindexVersion = currentVersion
      )
    })

    //TODO re-factor shared test state here into fixture method
    testRecords.foreach(Scanamo.put(dynamoDbClient)(table.name)(_))

    testRecords.map { record =>
      ReindexRequest(
        id = record.id,
        desiredVersion = desiredVersion,
        tableName = table.name
      )
    }
  }

  it("sends a notification for every record that needs a reindex") {
    withLocalSqsQueue { queue =>
      withLocalDynamoDbTable { table =>
        withLocalSnsTopic { topic =>
          val flags = snsLocalFlags(topic) ++ dynamoClientLocalFlags ++ sqsLocalFlags(
            queue)

          withServer(flags) { _ =>
            val expectedRecords = createReindexableData(queue, table)

            val reindexJob = createReindexJobWith(
              table = table,
              shardId = shardName,
              desiredVersion = desiredVersion
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
