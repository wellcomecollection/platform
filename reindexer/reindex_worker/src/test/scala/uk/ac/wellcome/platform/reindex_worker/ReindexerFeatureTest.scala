package uk.ac.wellcome.platform.reindex_worker

import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.models.Id
import uk.ac.wellcome.platform.reindex_worker.models.{
  CompletedReindexJob,
  ReindexJob,
  ReindexRecord
}
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

case class TestRecord(
  id: String,
  someData: String,
  version: Int,
  reindexShard: String,
  reindexVersion: Int
) extends Id

class ReindexerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with ExtendedPatience
    with fixtures.Server
    with LocalDynamoDbVersioned
    with SNS
    with SQS
    with ScalaFutures {

  val currentVersion = 1
  val desiredVersion = 5

  val shardName = "shard"

  private def createReindexableData(queue: Queue,
                                    table: Table): List[ReindexRecord] = {
    val numberOfRecords = 4

    val testRecords = (1 to numberOfRecords).map(i => {
      TestRecord(
        id = s"id$i",
        version = 1,
        someData = "A ghastly gharial ganking a green golem.",
        reindexShard = shardName,
        reindexVersion = currentVersion
      )
    })

    //TODO re-factor shared test state here into fixture method
    testRecords.foreach(Scanamo.put(dynamoDbClient)(table.name)(_))

    val expectedRecords = testRecords.map(
      (r: TestRecord) =>
        ReindexRecord(
          id = r.id,
          version = r.version + 1,
          reindexShard = shardName,
          reindexVersion = desiredVersion
      ))

    val reindexJob = ReindexJob(
      shardId = shardName,
      desiredVersion = desiredVersion
    )

    val sqsMessage = NotificationMessage(
      Subject = "",
      Message = toJson(reindexJob).get,
      TopicArn = "topic",
      MessageId = "message"
    )

    sqsClient.sendMessage(queue.url, toJson(sqsMessage).get)

    expectedRecords.toList
  }

  it("increases the reindexVersion on every record that needs a reindex") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalDynamoDbTable { table =>
          val flags
            : Map[String, String] = snsLocalFlags(topic) ++ dynamoDbLocalEndpointFlags(
            table) ++ sqsLocalFlags(queue)

          withServer(flags) { _ =>
            val expectedRecords =
              createReindexableData(queue, table)

            eventually {
              val actualRecords =
                Scanamo
                  .scan[ReindexRecord](dynamoDbClient)(table.name)
                  .map(_.right.get)

              actualRecords should contain theSameElementsAs expectedRecords
            }
          }
        }
      }
    }
  }
}
