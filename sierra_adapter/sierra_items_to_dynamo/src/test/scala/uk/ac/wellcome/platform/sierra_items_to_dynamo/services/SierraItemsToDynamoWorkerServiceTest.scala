package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import org.mockito.Mockito.{never, verify}
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.fixtures.{SNS, SQS}
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures.{
  SierraItemRecordVHSFixture,
  WorkerServiceFixture
}
import uk.ac.wellcome.platform.sierra_items_to_dynamo.merger.SierraItemRecordMerger
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.EmptyMetadata

class SierraItemsToDynamoWorkerServiceTest
    extends FunSpec
    with SNS
    with SQS
    with Matchers
    with Eventually
    with IntegrationPatience
    with MetricsSenderFixture
    with ScalaFutures
    with SierraGenerators
    with SierraItemRecordVHSFixture
    with WorkerServiceFixture {

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

    val expectedRecord = SierraItemRecordMerger.mergeItems(
      existingRecord = record1,
      updatedRecord = record2
    )

    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withItemRecordVHS(table, bucket) { versionedHybridStore =>
          storeSingleRecord(
            record1,
            versionedHybridStore = versionedHybridStore)

          withLocalSqsQueue { queue =>
            withLocalSnsTopic { topic =>
              withWorkerService(queue, table, bucket, topic) { _ =>
                sendNotificationToSQS(queue = queue, message = record2)

                eventually {
                  assertStored[SierraItemRecord](
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

  it("records a failure if it receives an invalid message") {
    withLocalDynamoDbTable { table =>
      withLocalS3Bucket { bucket =>
        withLocalSqsQueueAndDlq {
          case QueuePair(queue, dlq) =>
            withMockMetricSender { mockMetricsSender =>
              withLocalSnsTopic { topic =>
                withWorkerService(
                  queue,
                  table,
                  bucket,
                  topic,
                  mockMetricsSender) { _ =>
                  val body =
                    """
                    |{
                    | "something": "something"
                    |}
                  """.stripMargin

                  sendNotificationToSQS(queue = queue, body = body)

                  eventually {
                    assertQueueEmpty(queue)
                    assertQueueHasSize(dlq, size = 1)
                    verify(mockMetricsSender, never()).incrementCount(
                      "SierraItemsToDynamoWorkerService_ProcessMessage_failure")
                  }
                }
              }
            }
        }
      }
    }
  }

  def storeSingleRecord(
    itemRecord: SierraItemRecord,
    versionedHybridStore: SierraItemsVHS
  ): Assertion = {
    val putFuture =
      versionedHybridStore.updateRecord(id = itemRecord.id.withoutCheckDigit)(
        ifNotExisting = (itemRecord, EmptyMetadata())
      )(
        ifExisting = (existingRecord, existingMetadata) =>
          throw new RuntimeException(
            s"VHS should be empty; got ($existingRecord, $existingMetadata)!")
      )

    whenReady(putFuture) { _ =>
      val getFuture =
        versionedHybridStore.getRecord(id = itemRecord.id.withoutCheckDigit)
      whenReady(getFuture) { result =>
        result.get shouldBe itemRecord
      }
    }
  }
}
