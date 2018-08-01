package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import akka.actor.ActorSystem
import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.mockito.Mockito.{never, verify}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.{Queue, QueuePair}
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.sierra_items_to_dynamo.dynamo._
import uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures.DynamoInserterFixture
import uk.ac.wellcome.platform.sierra_items_to_dynamo.merger.SierraItemRecordMerger
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.json.JsonUtil._

class SierraItemsToDynamoWorkerServiceTest
    extends FunSpec
    with LocalVersionedHybridStore
    with SQS
    with Matchers
    with Eventually
    with ExtendedPatience
    with Akka
    with MetricsSenderFixture
    with ScalaFutures
    with SierraUtil {

  def withSierraWorkerService[R](
    versionedHybridStore: VersionedHybridStore[SierraItemRecord, EmptyMetadata, ObjectStore[SierraItemRecord]],
    queue: Queue,
    actorSystem: ActorSystem,
    metricsSender: MetricsSender
  )(testWith: TestWith[SierraItemsToDynamoWorkerService, R]): R =
    withSQSStream[NotificationMessage, R](actorSystem, queue, metricsSender) { sqsStream =>
      val dynamoInserter = new DynamoInserter(versionedHybridStore)
      val service = new SierraItemsToDynamoWorkerService(
        system = actorSystem,
        sqsStream = sqsStream,
        dynamoInserter = dynamoInserter
      )

      testWith(service)
    }

  def withItemRecordVHS[R](table: Table, bucket: Bucket)(
    testWith: TestWith[VersionedHybridStore[SierraItemRecord, EmptyMetadata, ObjectStore[SierraItemRecord]], R]): R =
    withTypeVHS[SierraItemRecord, EmptyMetadata, R](bucket, table, globalS3Prefix = "") { vhs =>
      testWith(vhs)
    }

  def withSierraWorkerService[R](
    testWith: TestWith[(QueuePair,
                        Table,
                        MetricsSender),
                       R]): Unit = {
    withActorSystem { actorSystem =>
      withLocalDynamoDbTable { table =>
        withLocalS3Bucket { bucket =>
          withTypeVHS[SierraItemRecord, EmptyMetadata, R](bucket, table, globalS3Prefix = "") { versionedHybridStore =>
            val dynamoInserter = new DynamoInserter(versionedHybridStore)
            withLocalSqsQueueAndDlq {
              case queuePair@QueuePair(queue, dlq) =>
                withMockMetricSender { metricsSender =>
                  withSQSStream[NotificationMessage, R](
                    actorSystem,
                    queue,
                    metricsSender) { sqsStream =>
                    val sierraItemsToDynamoWorkerService =
                      new SierraItemsToDynamoWorkerService(
                        system = actorSystem,
                        sqsStream = sqsStream,
                        dynamoInserter = dynamoInserter
                      )(actorSystem.dispatcher)

                    testWith(
                      (
                        queuePair,
                        table,
                        metricsSender
                      )
                    )
                  }
                }
            }
          }
        }
      }
    }
  }

  it("reads a sierra record from SQS and inserts it into DynamoDB") {
    val bibIds = createSierraBibNumbers(count = 5)

    val bibIds1 = List(bibIds(0), bibIds(1), bibIds(2))

    val record1 = createSierraItemRecordWith(
      modifiedDate = olderDate,
      bibIds = bibIds1
    )

    val bibIds2 = List(bibIds(2), bibIds(3), bibIds(4))

    val record2 = createSierraItemRecordWith(
      id = record1.id,
      modifiedDate = newerDate,
      bibIds = bibIds2
    )

    val expectedBibIds = List(bibIds(2), bibIds(3), bibIds(4))
    val expectedUnlinkedBibIds = List(bibIds(0), bibIds(1))

    val expectedRecord = SierraItemRecordMerger.mergeItems(
      existingRecord = record1,
      updatedRecord = record2
    )

    val expectedData = expectedRecord.data

    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withItemRecordVHS(table, bucket) { versionedHybridStore =>
          storeSingleRecord(
            versionedHybridStore = versionedHybridStore,
            id = record1.id.withoutCheckDigit,
            record = record1
          )

          withLocalSqsQueue { queue =>
            withActorSystem { actorSystem =>
              withMetricsSender(actorSystem) { metricsSender =>
                withSierraWorkerService(versionedHybridStore, queue, actorSystem, metricsSender) { _ =>
                  sendNotificationToSQS(queue = queue, message = record2)

                  eventually {
                    assertStored[SierraItemRecord](
                      bucket = bucket,
                      table = table,
                      id = record1.id.withoutCheckDigit,
                      record = expectedRecord
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  it("returns a GracefulFailureException if it receives an invalid message") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withItemRecordVHS(table, bucket) { versionedHybridStore =>
          withLocalSqsQueueAndDlq { case queuePair@QueuePair(queue, dlq) =>
            withActorSystem { actorSystem =>
              withMetricsSender(actorSystem) { metricsSender =>
                withSierraWorkerService(versionedHybridStore, queue, actorSystem, metricsSender) { _ =>
                  val body =
                    """
                      |{
                      | "something": "something"
                      |}
                    """.stripMargin

                  sendNotificationToSQS(queue = queue, body = body)

                  eventually {
                    assertQueueEmpty(queue)
                    assertQueueHasSize(dlq, size =1)
                    verify(metricsSender, never()).incrementCount(
                      "SierraItemsToDynamoWorkerService_ProcessMessage_failure")
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  def storeSingleRecord[T, Metadata](
                                      versionedHybridStore: VersionedHybridStore[T, Metadata, ObjectStore[T]],
                                      id: String,
                                      record: T,
                                      metadata: Metadata
                                    ): Assertion = {
    val putFuture = versionedHybridStore.updateRecord(id = id)(
      ifNotExisting = (record, metadata)
    )(
      ifExisting = (existingRecord, existingMetadata) =>
        throw new RuntimeException(s"VHS should be empty; got ($existingRecord, $existingMetadata)!")
    )

    whenReady(putFuture) { _ =>
      val getFuture = versionedHybridStore.getRecord(id = id)
      whenReady(getFuture) { result =>
        result.get shouldBe record
      }
    }
  }

  def storeSingleRecord[T](
                            versionedHybridStore: VersionedHybridStore[T, EmptyMetadata, ObjectStore[T]],
                            id: String,
                            record: T,
                          ): Assertion = storeSingleRecord[T, EmptyMetadata](
    versionedHybridStore = versionedHybridStore,
    id = id,
    record = record,
    metadata = EmptyMetadata()
  )
}
