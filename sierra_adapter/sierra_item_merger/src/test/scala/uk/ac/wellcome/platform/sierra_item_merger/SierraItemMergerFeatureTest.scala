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
import uk.ac.wellcome.storage.vhs.SourceMetadata
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.message.MessagePointer
import uk.ac.wellcome.sierra_adapter.utils.SierraMessagingHelpers
import uk.ac.wellcome.storage.ObjectLocation

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
    with SierraUtil
    with SierraMessagingHelpers {

  it("stores an item from SQS") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { sierraDataBucket =>
        withLocalS3Bucket { sierraItemsToDynamoBucket =>
          withLocalSnsTopic { topic =>
            withLocalDynamoDbTable { table =>
              val flags = vhsLocalFlags(sierraDataBucket, table) ++ snsLocalFlags(
                topic) ++ messageReaderLocalFlags(
                sierraItemsToDynamoBucket,
                queue)
              withServer(flags) { _ =>
                withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
                  sierraDataBucket,
                  table) { hybridStore =>
                  val bibId = createSierraBibNumber

                  val itemRecord = createSierraItemRecordWith(
                    bibIds = List(bibId)
                  )

                  sendNotification(
                    bucket = sierraItemsToDynamoBucket,
                    queue = queue,
                    itemRecord = itemRecord
                  )

                  val expectedSierraTransformable =
                    createSierraTransformableWith(
                      sierraId = bibId,
                      maybeBibRecord = None,
                      itemRecords = List(itemRecord)
                    )

                  eventually {
                    assertStoredAndSent(
                      transformable = expectedSierraTransformable,
                      topic = topic,
                      bucket = sierraDataBucket,
                      table = table
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

  it("stores multiple items from SQS") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { sierraDataBucket =>
        withLocalS3Bucket { sierraItemsToDynamoBucket =>
          withLocalSnsTopic { topic =>
            withLocalDynamoDbTable { table =>
              val flags = vhsLocalFlags(sierraDataBucket, table) ++ snsLocalFlags(
                topic) ++ messageReaderLocalFlags(
                sierraItemsToDynamoBucket,
                queue)
              withServer(flags) { _ =>
                withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
                  sierraDataBucket,
                  table) { hybridStore =>
                  val bibId1 = createSierraBibNumber
                  val itemRecord1 = createSierraItemRecordWith(
                    bibIds = List(bibId1)
                  )

                  sendNotification(
                    bucket = sierraItemsToDynamoBucket,
                    queue = queue,
                    itemRecord = itemRecord1
                  )

                  val bibId2 = createSierraBibNumber
                  val itemRecord2 = createSierraItemRecordWith(
                    bibIds = List(bibId2)
                  )

                  sendNotification(
                    bucket = sierraItemsToDynamoBucket,
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

                    assertStoredAndSent(
                      transformable = expectedSierraTransformable1,
                      topic = topic,
                      bucket = sierraDataBucket,
                      table = table
                    )
                    assertStoredAndSent(
                      transformable = expectedSierraTransformable2,
                      topic = topic,
                      bucket = sierraDataBucket,
                      table = table
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

  it("sends a notification for every transformable which changes") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { sierraDataBucket =>
        withLocalS3Bucket { sierraItemsToDynamoBucket =>
          withLocalSnsTopic { topic =>
            withLocalDynamoDbTable { table =>
              val flags = vhsLocalFlags(sierraDataBucket, table) ++ snsLocalFlags(
                topic) ++ messageReaderLocalFlags(
                sierraItemsToDynamoBucket,
                queue)
              withServer(flags) { _ =>
                withTypeVHS[
                  SierraTransformable,
                  SourceMetadata,
                  List[Assertion]](sierraDataBucket, table) { hybridStore =>
                  val bibIds = createSierraBibNumbers(3)
                  val itemRecord = createSierraItemRecordWith(
                    bibIds = bibIds
                  )

                  sendNotification(
                    bucket = sierraItemsToDynamoBucket,
                    queue = queue,
                    itemRecord = itemRecord
                  )

                  val expectedTransformables = bibIds.map { bibId =>
                    createSierraTransformableWith(
                      sierraId = bibId,
                      maybeBibRecord = None,
                      itemRecords = List(itemRecord)
                    )
                  }

                  eventually {
                    expectedTransformables.map { tranformable =>
                      assertStoredAndSent(
                        transformable = tranformable,
                        topic = topic,
                        bucket = sierraDataBucket,
                        table = table
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
  }

  private def sendNotification(
    bucket: Bucket,
    queue: Queue,
    itemRecord: SierraItemRecord): SendMessageResult = {
    val key = s"messaging/${randomAlphanumeric(10)}.json"
    s3Client.putObject(bucket.name, key, toJson(itemRecord).get)

    val messagePointer = MessagePointer(
      ObjectLocation(
        namespace = bucket.name,
        key = key
      )
    )

    sendNotificationToSQS(queue = queue, message = messagePointer)
  }
}
