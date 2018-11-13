package uk.ac.wellcome.platform.recorder.services

import com.gu.scanamo.Scanamo
import io.circe.Decoder
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.recorder.fixtures.WorkerServiceFixture
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.HybridRecord
import uk.ac.wellcome.test.fixtures.Akka

class RecorderWorkerServiceTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with Akka
    with LocalVersionedHybridStore
    with SQS
    with ScalaFutures
    with Messaging
    with MetricsSenderFixture
    with IntegrationPatience
    with WorkerServiceFixture
    with WorksGenerators {

  it("records an UnidentifiedWork") {
    withLocalDynamoDbTable { table =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { storageBucket =>
          withLocalS3Bucket { messagesBucket =>
            withLocalSqsQueue { queue =>
              withLocalSnsTopic { topic =>
                val work = createUnidentifiedWork
                sendMessage[TransformedBaseWork](
                  bucket = messagesBucket,
                  queue = queue,
                  obj = work
                )
                withWorkerService(
                  table,
                  storageBucket,
                  messagesBucket,
                  topic,
                  queue) { _ =>
                  eventually {
                    assertStoredSingleWork(topic, table, work)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  it("stores UnidentifiedInvisibleWorks") {
    withLocalDynamoDbTable { table =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { storageBucket =>
          withLocalS3Bucket { messagesBucket =>
            withLocalSqsQueue { queue =>
              withLocalSnsTopic { topic =>
                withWorkerService(
                  table,
                  storageBucket,
                  messagesBucket,
                  topic,
                  queue) { service =>
                  val invisibleWork = createUnidentifiedInvisibleWork
                  sendMessage[TransformedBaseWork](
                    bucket = messagesBucket,
                    queue = queue,
                    obj = invisibleWork
                  )
                  eventually {
                    assertStoredSingleWork(topic, table, invisibleWork)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  it("doesn't overwrite a newer work with an older work") {
    val olderWork = createUnidentifiedWork
    val newerWork = olderWork.copy(version = 10, title = "A nice new thing")

    withLocalDynamoDbTable { table =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { storageBucket =>
          withLocalS3Bucket { messagesBucket =>
            withLocalSqsQueue { queue =>
              withLocalSnsTopic { topic =>
                withWorkerService(
                  table,
                  storageBucket,
                  messagesBucket,
                  topic,
                  queue) { _ =>
                  sendMessage[TransformedBaseWork](
                    bucket = messagesBucket,
                    queue = queue,
                    obj = newerWork
                  )
                  eventually {
                    assertStoredSingleWork(topic, table, newerWork)
                    sendMessage(
                      bucket = messagesBucket,
                      queue = queue,
                      obj = olderWork
                    )
                    eventually {
                      assertStoredSingleWork(topic, table, newerWork)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  it("overwrites an older work with an newer work") {
    val olderWork = createUnidentifiedWork
    val newerWork = olderWork.copy(version = 10, title = "A nice new thing")

    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { storageBucket =>
        withLocalS3Bucket { messagesBucket =>
          withLocalSqsQueue { queue =>
            withLocalSnsTopic { topic =>
              withWorkerService(
                table,
                storageBucket,
                messagesBucket,
                topic,
                queue) { _ =>
                sendMessage[TransformedBaseWork](
                  bucket = messagesBucket,
                  queue = queue,
                  obj = olderWork
                )
                eventually {
                  assertStoredSingleWork(topic, table, olderWork)
                  sendMessage[TransformedBaseWork](
                    bucket = messagesBucket,
                    queue = queue,
                    obj = newerWork
                  )
                  eventually {
                    assertStoredSingleWork(
                      topic,
                      table,
                      newerWork,
                      expectedVhsVersion = 2)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  it("fails if saving to S3 fails") {
    withLocalDynamoDbTable { table =>
      withLocalSnsTopic { topic =>
        val badBucket = Bucket(name = "bad-bukkit")
        withLocalS3Bucket { messagesBucket =>
          withLocalSqsQueueAndDlq { case QueuePair(queue, dlq) =>
            withWorkerService(
              table,
              badBucket,
              messagesBucket,
              topic,
              queue) { _ =>
              val work = createUnidentifiedWork
              sendMessage[TransformedBaseWork](
                bucket = messagesBucket,
                queue = queue,
                obj = work
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
  }

  it("returns a failed Future if saving to DynamoDB fails") {
    val badTable = Table(name = "bad-table", index = "bad-index")
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { storageBucket =>
        withLocalS3Bucket { messagesBucket =>
          withLocalSqsQueueAndDlq { case QueuePair(queue, dlq) =>
            withWorkerService(
              badTable,
              storageBucket,
              messagesBucket,
              topic,
              queue) { service =>
              val work = createUnidentifiedWork
              sendMessage[TransformedBaseWork](
                bucket = messagesBucket,
                queue = queue,
                obj = work
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
  }

  private def assertStoredSingleWork[T <: TransformedBaseWork](
    topic: Topic,
    table: Table,
    expectedWork: T,
    expectedVhsVersion: Int = 1)(implicit decoder: Decoder[T]) = {
    val actualRecords: List[HybridRecord] =
      Scanamo
        .scan[HybridRecord](dynamoDbClient)(table.name)
        .map(_.right.get)

    actualRecords.size shouldBe 1

    val hybridRecord: HybridRecord = actualRecords.head
    hybridRecord.id shouldBe expectedWork.sourceIdentifier.toString
    hybridRecord.version shouldBe expectedVhsVersion

    val actualEntry = getObjectFromS3[T](
      bucket = Bucket(hybridRecord.location.namespace),
      key = hybridRecord.location.key
    )

    actualEntry shouldBe expectedWork
    getMessages[T](topic) should contain(expectedWork)
  }
}
