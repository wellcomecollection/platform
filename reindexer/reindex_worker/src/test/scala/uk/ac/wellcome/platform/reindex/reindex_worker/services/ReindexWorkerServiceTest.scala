package uk.ac.wellcome.platform.reindex.reindex_worker.services

import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.test.fixtures.{SNS, SQS}
import uk.ac.wellcome.platform.reindex.reindex_worker.fixtures.{
  DynamoFixtures,
  ReindexableTable,
  WorkerServiceFixture
}
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.reindex.reindex_worker.models.{
  CompleteReindexJob,
  ReindexJob
}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.vhs.HybridRecord

class ReindexWorkerServiceTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with Akka
    with DynamoFixtures
    with ReindexableTable
    with SNS
    with SQS
    with ScalaFutures
    with WorkerServiceFixture {

  val exampleRecord = HybridRecord(
    id = "id",
    version = 1,
    location = ObjectLocation(
      namespace = "s3://example-bukkit",
      key = "key.json.gz"
    )
  )

  it("completes a reindex") {
    withLocalDynamoDbTable { table =>
      withLocalSnsTopic { topic =>
        withLocalSqsQueueAndDlq {
          case QueuePair(queue, dlq) =>
            withWorkerService(queue, table, topic) { _ =>
              val reindexJob =
                CompleteReindexJob(segment = 0, totalSegments = 1)

              Scanamo.put(dynamoDbClient)(table.name)(exampleRecord)

              sendNotificationToSQS[ReindexJob](
                queue = queue,
                message = reindexJob
              )

              eventually {
                val actualRecords: Seq[HybridRecord] =
                  listMessagesReceivedFromSNS(topic)
                    .map {
                      _.message
                    }
                    .map {
                      fromJson[HybridRecord](_).get
                    }
                    .distinct

                actualRecords shouldBe List(exampleRecord)
                assertQueueEmpty(queue)
                assertQueueEmpty(dlq)
              }
            }
        }
      }
    }
  }

  it("fails if it cannot parse the SQS message as a ReindexJob") {
    withLocalDynamoDbTable { table =>
      withLocalSnsTopic { topic =>
        withLocalSqsQueueAndDlq {
          case QueuePair(queue, dlq) =>
            withWorkerService(queue, table, topic) { _ =>
              sendNotificationToSQS(
                queue = queue,
                body = "<xml>What is JSON.</xl?>"
              )

              eventually {
                assertQueueEmpty(queue)
                assertQueueHasSize(dlq, 1)
              }
            }
        }
      }
    }
  }

  it("fails if the reindex job fails") {
    val badTable = Table(name = "doesnotexist", index = "whatindex")
    val badTopic = Topic("does-not-exist")

    withLocalSqsQueueAndDlq {
      case QueuePair(queue, dlq) =>
        withWorkerService(queue, badTable, badTopic) { _ =>
          val reindexJob = CompleteReindexJob(segment = 5, totalSegments = 10)

          sendNotificationToSQS[ReindexJob](
            queue = queue,
            message = reindexJob
          )

          eventually {
            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, 1)
          }
        }
    }
  }
}
