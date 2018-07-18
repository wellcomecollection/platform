package uk.ac.wellcome.platform.sierra_item_merger

import org.scalatest.concurrent.Eventually
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.storage.fixtures.{LocalVersionedHybridStore, S3}
import uk.ac.wellcome.storage.vhs.SourceMetadata
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

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
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table)
          withServer(flags) { _ =>
            withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
              bucket,
              table) { hybridStore =>
              val record = createSierraItemRecord
              sendNotificationToSQS(queue = queue, message = record)

              val expectedSierraTransformable = SierraTransformable(
                sierraId = createSierraRecordNumber,
                itemData = Map(record.id -> record)
              )

              eventually {
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = expectedSierraTransformable.id,
                  record = expectedSierraTransformable)
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
              val bibId1 = createSierraRecordNumber
              val record1 = createSierraItemRecordWith(bibIds = List(bibId1))
              sendNotificationToSQS(queue = queue, message = record1)

              val bibId2 = createSierraRecordNumber
              val record2 = createSierraItemRecordWith(bibIds = List(bibId2))
              sendNotificationToSQS(queue = queue, message = record2)

              eventually {
                val expectedSierraTransformable1 = SierraTransformable(
                  sierraId = bibId1,
                  itemData = Map(record1.id -> record1)
                )

                val expectedSierraTransformable2 = SierraTransformable(
                  sierraId = bibId2,
                  itemData = Map(record2.id -> record2)
                )

                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = expectedSierraTransformable1.id,
                  record = expectedSierraTransformable1)
                assertStored[SierraTransformable](
                  bucket,
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
