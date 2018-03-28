package uk.ac.wellcome.platform.reindex_worker

import com.gu.scanamo.{DynamoFormat, Scanamo}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.Matchers
import org.scalatest.FunSpec
import uk.ac.wellcome.models.{Id, Versioned}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.platform.reindex_worker.models.{
  CompletedReindexJob,
  ReindexJob,
  ReindexRecord
}
import uk.ac.wellcome.test.fixtures.{LocalDynamoDb, SNS, SQS}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._
import scala.collection.JavaConversions._

case class TestRecord(
  id: String,
  someData: String,
  version: Int,
  reindexShard: String,
  reindexVersion: Int
) extends Versioned
    with Id

class ReindexerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with ExtendedPatience
    with fixtures.Server
    with LocalDynamoDb[TestRecord]
    with SNS
    with SQS
    with ScalaFutures {

  override lazy val evidence: DynamoFormat[TestRecord] =
    DynamoFormat[TestRecord]

  val currentVersion = 1
  val desiredVersion = 5

  val shardName = "shard"

  private def createReindexableData(queueUrl: String,
                                    tableName: String): List[ReindexRecord] = {
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
    testRecords.foreach(Scanamo.put(dynamoDbClient)(tableName)(_))

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

    val sqsMessage = SQSMessage(
      None,
      toJson(reindexJob).get,
      "topic",
      "message",
      "now"
    )

    sqsClient.sendMessage(queueUrl, toJson(sqsMessage).get)

    expectedRecords.toList
  }

  it("increases the reindexVersion on every record that needs a reindex") {
    withLocalSqsQueue { queueUrl =>
      withLocalSnsTopic { topicArn =>
        withLocalDynamoDbTableAndIndex { fixtures =>
          val tableName = fixtures.tableName
          val indexName = fixtures.indexName

          val flags
            : Map[String, String] = snsLocalFlags(topicArn) ++ dynamoDbLocalEndpointFlags(
            tableName) ++ sqsLocalFlags(queueUrl) ++ Map(
            "aws.dynamo.indexName" -> indexName)

          withServer(flags) { _ =>
            val expectedRecords =
              createReindexableData(queueUrl = queueUrl, tableName = tableName)

            eventually {
              val actualRecords =
                Scanamo
                  .scan[ReindexRecord](dynamoDbClient)(tableName)
                  .map(_.right.get)

              actualRecords should contain theSameElementsAs expectedRecords
            }
          }
        }
      }
    }
  }

  it("sends an SNS notice for a completed reindex") {
    withLocalSqsQueue { queueUrl =>
      withLocalSnsTopic { topicArn =>
        withLocalDynamoDbTableAndIndex { fixtures =>
          val tableName = fixtures.tableName
          val indexName = fixtures.indexName

          val flags
            : Map[String, String] = snsLocalFlags(topicArn) ++ dynamoDbLocalEndpointFlags(
            tableName) ++ sqsLocalFlags(queueUrl) ++ Map(
            "aws.dynamo.indexName" -> indexName)

          withServer(flags) { _ =>
            val expectedRecords =
              createReindexableData(queueUrl = queueUrl, tableName = tableName)

            val expectedMessage = CompletedReindexJob(
              shardId = shardName,
              completedReindexVersion = desiredVersion
            )

            eventually {
              val messages = listMessagesReceivedFromSNS(topicArn)

              messages should have size 1

              JsonUtil
                .fromJson[CompletedReindexJob](
                  messages.head.message
                )
                .get shouldBe expectedMessage
            }
          }
        }
      }
    }
  }

  it("does not send a message if it cannot complete a reindex") {
    withLocalSqsQueue { queueUrl =>
      withLocalSnsTopic { topicArn =>
        withLocalDynamoDbTableAndIndex { fixtures =>
          val tableName = fixtures.tableName
          val indexName = fixtures.indexName

          val flags
            : Map[String, String] = snsLocalFlags(topicArn) ++ dynamoDbLocalEndpointFlags(
            "non_existent_table") ++ sqsLocalFlags(queueUrl) ++ Map(
            "aws.dynamo.indexName" -> indexName)

          withServer(flags) { _ =>
            val expectedRecords =
              createReindexableData(queueUrl = queueUrl, tableName = tableName)

            // We wait some time to ensure that the message is not processed
            Thread.sleep(5000)

            val messages = listMessagesReceivedFromSNS(topicArn)
            messages should have size 0
          }
        }
      }
    }
  }
}
