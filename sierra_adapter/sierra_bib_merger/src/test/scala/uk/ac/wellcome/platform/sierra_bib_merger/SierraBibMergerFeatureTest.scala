package uk.ac.wellcome.platform.sierra_bib_merger

import io.circe.Encoder
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
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
    with LocalVersionedHybridStore
    with SierraUtil {

  implicit val encoder = Encoder[SierraTransformable]

  it("stores a bib in the hybrid store") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table)
          withServer(flags) { _ =>
            withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
              bucket,
              table) { hybridStore =>
              val bibRecord = createSierraBibRecord

              sendNotificationToSQS(queue = queue, message = bibRecord)

              val expectedSierraTransformable =
                SierraTransformable(bibRecord = bibRecord)

              eventually {
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = expectedSierraTransformable.id,
                  bibRecord = expectedSierraTransformable)
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

              val expectedTransformable1 =
                SierraTransformable(bibRecord = record1)

              val record2 = createSierraBibRecord

              sendNotificationToSQS(queue = queue, message = record2)

              val expectedTransformable2 =
                SierraTransformable(bibRecord = record2)

              eventually {
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = expectedTransformable1.id,
                  record = expectedTransformable1)
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = expectedTransformable2.id,
                  record = expectedTransformable2)
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
              val id = "3000003"
              val oldBibRecord = createSierraBibRecordWith(
                modifiedDate = olderDate
              )

              val oldTransformable = SierraTransformable(bibRecord = oldBibRecord)

              val newBibRecord = createSierraBibRecordWith(
                id = oldBibRecord.id,
                modifiedDate = newerDate
              )

              hybridStore
                .updateRecord(oldTransformable.id)(
                  (oldTransformable, SourceMetadata(oldTransformable.sourceName)))((t, m) =>
                  (t, m))
                .map { _ =>
                  sendNotificationToSQS(queue = queue, message = newBibRecord)
                }

              val expectedTransformable =
                SierraTransformable(bibRecord = newBibRecord)

              eventually {
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = expectedTransformable.id,
                  newBibRecord = expectedTransformable)
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
                modifiedDate = newerDate
              )

              val expectedTransformable =
                SierraTransformable(bibRecord = newBibRecord)

              val oldBibRecord = createSierraBibRecordWith(
                id = newBibRecord.id,
                modifiedDate = olderDate
              )

              hybridStore
                .updateRecord(expectedTransformable.id)((
                  expectedTransformable,
                  SourceMetadata(expectedTransformable.sourceName)))(
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
                id = expectedTransformable.id,
                record = expectedTransformable)
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
              val id = createSierraRecordNumberString
              val transformable = SierraTransformable(sourceId = id)

              val bibRecord = createSierraBibRecordWith(id = id)

              val future =
                hybridStore.updateRecord(transformable.id)(
                  (transformable, SourceMetadata(transformable.sourceName)))((t, m) =>
                  (t, m))

              future.map { _ =>
                sendNotificationToSQS(queue = queue, message = bibRecord)
              }

              val expectedSierraTransformable =
                SierraTransformable(bibRecord = bibRecord)

              eventually {
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = expectedSierraTransformable.id,
                  bibRecord = expectedSierraTransformable)
              }
            }
          }
        }
      }
    }
  }
}
