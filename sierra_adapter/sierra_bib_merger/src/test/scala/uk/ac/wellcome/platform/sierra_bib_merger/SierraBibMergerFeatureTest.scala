package uk.ac.wellcome.platform.sierra_bib_merger

import io.circe.Encoder
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.SourceMetadata
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

class SierraBibMergerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with MockitoSugar
    with ExtendedPatience
    with ScalaFutures
    with SQS
    with fixtures.Server
    with SierraUtil
    with LocalVersionedHybridStore {

  implicit val encoder = Encoder[SierraTransformable]

  it("should store a bib in the hybrid store") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table)
          withServer(flags) { _ =>
            withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
              bucket,
              table) { hybridStore =>
              val record = createSierraBibRecord

              sendNotificationToSQS(queue = queue, message = record)

              val expectedSierraTransformable =
                SierraTransformable(bibRecord = record)

              eventually {
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  expectedSierraTransformable)
              }
            }
          }
        }
      }
    }
  }

  it("stores multiple bibs from SQS") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table)
          withServer(flags) { _ =>
            withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
              bucket,
              table) { hybridStore =>

              val record1 = createSierraBibRecord
              sendNotificationToSQS(queue = queue, message = record1)

              val expectedSierraTransformable1 =
                SierraTransformable(bibRecord = record1)

              val record2 = createSierraBibRecord
              sendNotificationToSQS(queue = queue, message = record2)

              val expectedSierraTransformable2 =
                SierraTransformable(bibRecord = record2)

              eventually {
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  expectedSierraTransformable1)
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  expectedSierraTransformable2)
              }
            }
          }
        }
      }
    }
  }

  it("updates a bib if a newer version is sent to SQS") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table)
          withServer(flags) { _ =>
            withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
              bucket,
              table) { hybridStore =>
              val oldBibRecord = createSierraBibRecordWith(
                data = "<<old data>>",
                modifiedDate = olderDate
              )
              val oldRecord = SierraTransformable(bibRecord = oldBibRecord)

              val record = createSierraBibRecordWith(
                id = oldBibRecord.id,
                data = "<<newer data>>",
                modifiedDate = newerDate
              )

              hybridStore
                .updateRecord(oldRecord.id)(
                  (oldRecord, SourceMetadata(oldRecord.sourceName)))((t, m) =>
                  (t, m))
                .map { _ =>
                  sendNotificationToSQS(queue = queue, message = record)
                }

              val expectedSierraTransformable =
                SierraTransformable(bibRecord = record)

              eventually {
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  expectedSierraTransformable)
              }
            }
          }
        }
      }
    }
  }

  it("does not update a bib if an older version is sent to SQS") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table)
          withServer(flags) { _ =>
            withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
              bucket,
              table) { hybridStore =>
              val newBibRecord = createSierraBibRecordWith(
                data = "<<newer data>>",
                modifiedDate = newerDate
              )

              val expectedSierraTransformable =
                SierraTransformable(bibRecord = newBibRecord)

              val oldBibRecord = createSierraBibRecordWith(
                id = newBibRecord.id,
                data = "<<old data>>",
                modifiedDate = olderDate
              )

              hybridStore
                .updateRecord(expectedSierraTransformable.id)((
                  expectedSierraTransformable,
                  SourceMetadata(expectedSierraTransformable.sourceName)))(
                  (t, m) => (t, m))
                .map { _ =>
                  sendNotificationToSQS(queue = queue, message = oldBibRecord)
                }

              // Blocking in Scala is generally a bad idea; we do it here so there's
              // enough time for this update to have gone through (if it was going to).
              Thread.sleep(5000)

              assertStored[SierraTransformable](
                bucket,
                table,
                expectedSierraTransformable)
            }
          }
        }
      }
    }
  }

  it("stores a bib from SQS if the ID already exists but no bibData") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table)
          withServer(flags) { _ =>
            withTypeVHS[SierraTransformable, SourceMetadata, Unit](
              bucket,
              table) { hybridStore =>
              val newRecord = SierraTransformable(sourceId = "7000007")

              val record = createSierraBibRecordWith(id = newRecord.sourceId)

              val future =
                hybridStore.updateRecord(newRecord.id)(
                  (newRecord, SourceMetadata(newRecord.sourceName)))((t, m) =>
                  (t, m))

              future.map { _ =>
                sendNotificationToSQS(queue = queue, message = record)
              }

              val expectedSierraTransformable =
                SierraTransformable(bibRecord = record)

              eventually {
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  expectedSierraTransformable)
              }
            }
          }
        }
      }
    }
  }
}
