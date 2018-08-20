package uk.ac.wellcome.platform.recorder.services

import com.gu.scanamo.Scanamo
import io.circe.Decoder
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.SQS.{Queue, QueuePair}
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, HybridRecord}
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

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
    with WorksUtil {

  it("records an UnidentifiedWork") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { storageBucket =>
        withLocalS3Bucket { messagesBucket =>
          withLocalSqsQueue { queue =>
            val work = createUnidentifiedWork
            sendMessage[TransformedBaseWork](
              bucket = messagesBucket,
              queue = queue,
              obj = work
            )
            withRecorderWorkerService(
              table,
              storageBucket,
              messagesBucket,
              queue) { _ =>
              eventually {
                assertStoredSingleWork(storageBucket, table, work)
              }
            }
          }
        }
      }
    }
  }

  it("stores UnidentifiedInvisibleWorks") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { storageBucket =>
        withLocalS3Bucket { messagesBucket =>
          withLocalSqsQueue { queue =>
            withRecorderWorkerService(
              table,
              storageBucket,
              messagesBucket,
              queue) { service =>
              val invisibleWork = createUnidentifiedInvisibleWork
              sendMessage[TransformedBaseWork](
                bucket = messagesBucket,
                queue = queue,
                obj = invisibleWork
              )
              eventually {
                assertStoredSingleWork(storageBucket, table, invisibleWork)
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
      withLocalS3Bucket { storageBucket =>
        withLocalS3Bucket { messagesBucket =>
          withLocalSqsQueue { queue =>
            withRecorderWorkerService(
              table,
              storageBucket,
              messagesBucket,
              queue) { service =>
              sendMessage[TransformedBaseWork](
                bucket = messagesBucket,
                queue = queue,
                obj = newerWork
              )
              eventually {
                assertStoredSingleWork(storageBucket, table, newerWork)
                sendMessage(
                  bucket = messagesBucket,
                  queue = queue,
                  obj = olderWork
                )
                eventually {
                  assertStoredSingleWork(storageBucket, table, newerWork)
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
            withRecorderWorkerService(
              table,
              storageBucket,
              messagesBucket,
              queue) { service =>
              sendMessage[TransformedBaseWork](
                bucket = messagesBucket,
                queue = queue,
                obj = olderWork
              )
              eventually {
                assertStoredSingleWork(storageBucket, table, olderWork)
                sendMessage[TransformedBaseWork](
                  bucket = messagesBucket,
                  queue = queue,
                  obj = newerWork
                )
                eventually {
                  assertStoredSingleWork(
                    storageBucket,
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
      val badBucket = Bucket(name = "bad-bukkit")
      withLocalS3Bucket { messagesBucket =>
        withLocalSqsQueueAndDlq {
          case QueuePair(queue, dlq) =>
            withRecorderWorkerService(table, badBucket, messagesBucket, queue) {
              service =>
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

  it("returns a failed Future if saving to DynamoDB fails") {
    val badTable = Table(name = "bad-table", index = "bad-index")
    withLocalS3Bucket { storageBucket =>
      withLocalS3Bucket { messagesBucket =>
        withLocalSqsQueueAndDlq {
          case QueuePair(queue, dlq) =>
            withRecorderWorkerService(
              badTable,
              storageBucket,
              messagesBucket,
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

  private def assertStoredSingleWork[T <: TransformedBaseWork](
    bucket: Bucket,
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
      bucket = bucket,
      key = hybridRecord.s3key
    )
    actualEntry shouldBe expectedWork
  }

  private def withRecorderWorkerService[R](
    table: Table,
    storageBucket: Bucket,
    messagesBucket: Bucket,
    queue: Queue)(testWith: TestWith[RecorderWorkerService, R]) = {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withTypeVHS[TransformedBaseWork, EmptyMetadata, R](
          bucket = storageBucket,
          table = table) { versionedHybridStore =>
          withMessageStream[TransformedBaseWork, R](
            actorSystem,
            messagesBucket,
            queue,
            metricsSender) { messageStream =>
            val workerService = new RecorderWorkerService(
              versionedHybridStore = versionedHybridStore,
              messageStream = messageStream,
              system = actorSystem
            )

            try {
              testWith(workerService)
            } finally {
              workerService.stop()
            }
          }
        }
      }
    }
  }
}
