package uk.ac.wellcome.platform.recorder.services

import com.gu.scanamo.Scanamo
import io.circe.Decoder
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.fixtures.{Messaging, SQS}
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
          withLocalSqsQueue { queue =>
            withLocalSnsTopic { topic =>
              val work = createUnidentifiedWork
              sendMessage[TransformedBaseWork](queue = queue, obj = work)
              withWorkerService(table, storageBucket, topic, queue) { _ =>
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

  it("stores UnidentifiedInvisibleWorks") {
    withLocalDynamoDbTable { table =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { storageBucket =>
          withLocalSqsQueue { queue =>
            withLocalSnsTopic { topic =>
              withWorkerService(table, storageBucket, topic, queue) { service =>
                val invisibleWork = createUnidentifiedInvisibleWork
                sendMessage[TransformedBaseWork](queue = queue, invisibleWork)
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

  it("doesn't overwrite a newer work with an older work") {
    val olderWork = createUnidentifiedWork
    val newerWork = olderWork.copy(version = 10, title = "A nice new thing")

    withLocalDynamoDbTable { table =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { storageBucket =>
          withLocalSqsQueue { queue =>
            withLocalSnsTopic { topic =>
              withWorkerService(table, storageBucket, topic, queue) { _ =>
                sendMessage[TransformedBaseWork](queue = queue, newerWork)
                eventually {
                  assertStoredSingleWork(topic, table, newerWork)
                  sendMessage[TransformedBaseWork](
                    queue = queue,
                    obj = olderWork)
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

  it("overwrites an older work with an newer work") {
    val olderWork = createUnidentifiedWork
    val newerWork = olderWork.copy(version = 10, title = "A nice new thing")

    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { storageBucket =>
        withLocalSqsQueue { queue =>
          withLocalSnsTopic { topic =>
            withWorkerService(table, storageBucket, topic, queue) { _ =>
              sendMessage[TransformedBaseWork](queue = queue, obj = olderWork)
              eventually {
                assertStoredSingleWork(topic, table, olderWork)
                sendMessage[TransformedBaseWork](queue = queue, obj = newerWork)
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

  it("fails if saving to S3 fails") {
    withLocalDynamoDbTable { table =>
      withLocalSnsTopic { topic =>
        val badBucket = Bucket(name = "bad-bukkit")
        withLocalSqsQueueAndDlq {
          case QueuePair(queue, dlq) =>
            withWorkerService(table, badBucket, topic, queue) { _ =>
              val work = createUnidentifiedWork
              sendMessage[TransformedBaseWork](queue = queue, obj = work)
              eventually {
                assertQueueEmpty(queue)
                assertQueueHasSize(dlq, 1)
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
        withLocalSqsQueueAndDlq {
          case QueuePair(queue, dlq) =>
            withWorkerService(badTable, storageBucket, topic, queue) { _ =>
              val work = createUnidentifiedWork
              sendMessage[TransformedBaseWork](queue = queue, obj = work)
              eventually {
                assertQueueEmpty(queue)
                assertQueueHasSize(dlq, 1)
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

    val actualEntry = getObjectFromS3[T](hybridRecord.location)

    actualEntry shouldBe expectedWork
    getMessages[T](topic) should contain(expectedWork)
  }
}
