package uk.ac.wellcome.platform.sierra_item_merger

import org.scalatest.concurrent.Eventually
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.platform.sierra_item_merger.utils.SierraItemMergerTestUtil
import uk.ac.wellcome.storage.fixtures.{LocalVersionedHybridStore, S3}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.SourceMetadata
import uk.ac.wellcome.test.utils.{ExtendedPatience, JsonTestUtil}
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class SierraItemMergerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with ExtendedPatience
    with JsonTestUtil
    with fixtures.Server
    with SQS
    with S3
    with LocalVersionedHybridStore
    with SierraItemMergerTestUtil {

  it("stores an item from SQS") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table)
          withServer(flags) { _ =>
            withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
              bucket,
              table) { hybridStore =>
              val id = "i1000001"
              val bibId = "b1000001"

              val record = sierraItemRecord(
                id = id,
                updatedDate = "2001-01-01T01:01:01Z",
                bibIds = List(bibId)
              )

              sendItemRecordToSQS(record, queue)

              val expectedSierraTransformable = SierraTransformable(
                sourceId = bibId,
                itemData = Map(id -> record)
              )

              eventually {
                assertStored(bucket, table, expectedSierraTransformable)
              }
            }
          }
        }
      }
    }
  }

  it("stores multiple items from SQS") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table)
          withServer(flags) { _ =>
            withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
              bucket,
              table) { hybridStore =>
              val bibId1 = "b1000001"

              val id1 = "1000001"

              val record1 = sierraItemRecord(
                id = id1,
                updatedDate = "2001-01-01T01:01:01Z",
                bibIds = List(bibId1)
              )

              sendItemRecordToSQS(record1, queue)

              val bibId2 = "b2000002"
              val id2 = "2000002"

              val record2 = sierraItemRecord(
                id = id2,
                updatedDate = "2002-02-02T02:02:02Z",
                bibIds = List(bibId2)
              )

              sendItemRecordToSQS(record2, queue)

              eventually {
                val expectedSierraTransformable1 = SierraTransformable(
                  sourceId = bibId1,
                  itemData = Map(id1 -> record1)
                )

                val expectedSierraTransformable2 = SierraTransformable(
                  sourceId = bibId2,
                  itemData = Map(id2 -> record2)
                )

                assertStored(bucket, table, expectedSierraTransformable1)
                assertStored(bucket, table, expectedSierraTransformable2)
              }
            }
          }
        }
      }
    }
  }

  private def assertStored(bucket: Bucket,
                           table: Table,
                           record: SierraTransformable) =
    assertJsonStringsAreEqual(
      getJsonFor(bucket, table, id = record.id),
      toJson(record).get
    )

  private def sendItemRecordToSQS(itemRecord: SierraItemRecord, queue: Queue) = {
    val message = NotificationMessage(
      MessageId = "message-id",
      TopicArn = "topic",
      Subject = "Test message sent by SierraItemMergerWorkerServiceTest",
      Message = toJson(itemRecord).get
    )
    sqsClient.sendMessage(queue.url, toJson(message).get)
  }
}
