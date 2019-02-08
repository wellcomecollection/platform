package uk.ac.wellcome.platform.reindex.reindex_worker.services

import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.fixtures.{SNS, SQS}
import uk.ac.wellcome.platform.reindex.reindex_worker.fixtures.{
  DynamoFixtures,
  ReindexableTable,
  WorkerServiceFixture
}
import uk.ac.wellcome.test.fixtures.Akka
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.reindex.reindex_worker.models.CompleteReindexParameters
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
              val reindexParameters = CompleteReindexParameters(
                segment = 0,
                totalSegments = 1
              )

              Scanamo.put(dynamoDbClient)(table.name)(exampleRecord)

              sendNotificationToSQS(
                queue = queue,
                message =
                  createReindexRequestWith(parameters = reindexParameters)
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
          sendNotificationToSQS(queue = queue, message = createReindexRequest)

          eventually {
            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, 1)
          }
        }
    }
  }

  it("fails if passed an invalid job ID") {
    withLocalDynamoDbTable { table =>
      withLocalSnsTopic { topic =>
        withLocalSqsQueueAndDlq {
          case QueuePair(queue, dlq) =>
            withWorkerService(queue, configMap = Map("foo" -> ((table, topic)))) {
              _ =>
                sendNotificationToSQS(
                  queue = queue,
                  message = createReindexRequestWith(jobConfigId = "bar")
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

  it("selects the correct job config") {
    withLocalDynamoDbTable { table1 =>
      withLocalSnsTopic { topic1 =>
        withLocalDynamoDbTable { table2 =>
          withLocalSnsTopic { topic2 =>
            withLocalSqsQueueAndDlq {
              case QueuePair(queue, dlq) =>
                val exampleRecord1 = exampleRecord.copy(id = "exampleRecord1")
                val exampleRecord2 = exampleRecord.copy(id = "exampleRecord2")

                Scanamo.put(dynamoDbClient)(table1.name)(exampleRecord1)
                Scanamo.put(dynamoDbClient)(table2.name)(exampleRecord2)

                val configMap = Map(
                  "1" -> ((table1, topic1)),
                  "2" -> ((table2, topic2))
                )
                withWorkerService(queue, configMap = configMap) { _ =>
                  sendNotificationToSQS(
                    queue = queue,
                    message = createReindexRequestWith(jobConfigId = "1")
                  )

                  eventually {
                    val actualRecords: Seq[HybridRecord] =
                      listMessagesReceivedFromSNS(topic1)
                        .map {
                          _.message
                        }
                        .map {
                          fromJson[HybridRecord](_).get
                        }
                        .distinct

                    actualRecords shouldBe List(exampleRecord1)

                    assertSnsReceivesNothing(topic2)

                    assertQueueEmpty(queue)
                    assertQueueEmpty(dlq)
                  }

                  sendNotificationToSQS(
                    queue = queue,
                    message = createReindexRequestWith(jobConfigId = "2")
                  )

                  eventually {
                    val actualRecords: Seq[HybridRecord] =
                      listMessagesReceivedFromSNS(topic2)
                        .map {
                          _.message
                        }
                        .map {
                          fromJson[HybridRecord](_).get
                        }
                        .distinct

                    actualRecords shouldBe List(exampleRecord2)

                    assertQueueEmpty(queue)
                    assertQueueEmpty(dlq)
                  }
                }
            }
          }
        }
      }
    }
  }
}
