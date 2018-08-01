package uk.ac.wellcome.platform.sierra_item_merger

import com.amazonaws.services.sqs.model.SendMessageResult
import org.scalatest.concurrent.Eventually
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.SierraTransformable._
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.fixtures.{LocalVersionedHybridStore, S3}
import uk.ac.wellcome.storage.vhs.{HybridRecord, SourceMetadata}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class SierraItemMergerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with ExtendedPatience
    with fixtures.Server
    with SQS
    with S3
    with LocalVersionedHybridStore
    with SierraUtil {

  it("stores an item from SQS") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { vhsBucket =>
        withLocalS3Bucket { messagingBucket =>
          withLocalDynamoDbTable { table =>
            val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(vhsBucket, table) ++ s3LocalFlags(messagingBucket)
            withServer(flags) { _ =>
              withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
                vhsBucket,
                table) { hybridStore =>
                val bibId = createSierraBibNumber

                val itemRecord = createSierraItemRecordWith(
                  bibIds = List(bibId)
                )

                sendNotification(
                  bucket = messagingBucket,
                  queue = queue,
                  itemRecord = itemRecord
                )

                val expectedSierraTransformable = createSierraTransformableWith(
                  sierraId = bibId,
                  maybeBibRecord = None,
                  itemRecords = List(itemRecord)
                )

                eventually {
                  assertStored[SierraTransformable](
                    bucket = vhsBucket,
                    table = table,
                    id = expectedSierraTransformable.id,
                    record = expectedSierraTransformable)
                }
              }
            }
          }
        }
      }
    }
  }

  it("stores multiple items from SQS") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { vhsBucket =>
        withLocalS3Bucket { messagingBucket =>
          withLocalDynamoDbTable { table =>
            val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(vhsBucket, table) ++ s3LocalFlags(messagingBucket)
            withServer(flags) { _ =>
              withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
                vhsBucket,
                table) { hybridStore =>
                val bibId1 = createSierraBibNumber
                val itemRecord1 = createSierraItemRecordWith(
                  bibIds = List(bibId1)
                )

                sendNotification(
                  bucket = messagingBucket,
                  queue = queue,
                  itemRecord = itemRecord1
                )

                val bibId2 = createSierraBibNumber
                val itemRecord2 = createSierraItemRecordWith(
                  bibIds = List(bibId2)
                )

                sendNotification(
                  bucket = messagingBucket,
                  queue = queue,
                  itemRecord = itemRecord2
                )

                eventually {
                  val expectedSierraTransformable1 =
                    createSierraTransformableWith(
                      sierraId = bibId1,
                      maybeBibRecord = None,
                      itemRecords = List(itemRecord1)
                    )

                  val expectedSierraTransformable2 =
                    createSierraTransformableWith(
                      sierraId = bibId2,
                      maybeBibRecord = None,
                      itemRecords = List(itemRecord2)
                    )

                  assertStored[SierraTransformable](
                    vhsBucket,
                    table,
                    id = expectedSierraTransformable1.id,
                    record = expectedSierraTransformable1)
                  assertStored[SierraTransformable](
                    vhsBucket,
                    table,
                    id = expectedSierraTransformable2.id,
                    record = expectedSierraTransformable2)
                }
              }
            }
          }
        }
      }
    }
  }

  private def sendNotification(bucket: Bucket, queue: Queue, itemRecord: SierraItemRecord): SendMessageResult = {
    val key = s"messaging/${randomAlphanumeric(10)}.json"
    s3Client.putObject(bucket.name, key, toJson(itemRecord).get)

    val hybridRecord = HybridRecord(
      id = itemRecord.id.withoutCheckDigit,
      s3key = key,
      version = 1
    )

    val notification = createNotificationMessageWith(hybridRecord)

    sendNotificationToSQS(queue = queue, message = notification)
  }
}
