package uk.ac.wellcome.platform.sierra_bib_merger

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.SierraTransformable._
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.EmptyMetadata
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.sierra_adapter.utils.SierraVHSUtil

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
    with SierraUtil
    with SierraVHSUtil {

  it("stores a bib in the hybrid store") {
    withLocalSqsQueue { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          val flags = sqsLocalFlags(queue) ++ vhsLocalFlags(bucket, table)
          withServer(flags) { _ =>
            withTypeVHS[SierraTransformable, EmptyMetadata, Assertion](
              bucket,
              table) { hybridStore =>
              val bibRecord = createSierraBibRecord

              sendNotificationToSQS(queue = queue, message = bibRecord)

              val expectedSierraTransformable =
                SierraTransformable(bibRecord = bibRecord)

              eventually {
                assertStored(
                  transformable = expectedSierraTransformable,
                  bucket = bucket,
                  table = table
                )
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
            withTypeVHS[SierraTransformable, EmptyMetadata, Assertion](
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
                assertStored(
                  transformable = expectedTransformable1,
                  bucket = bucket,
                  table = table
                )
                assertStored(
                  transformable = expectedTransformable2,
                  bucket = bucket,
                  table = table
                )
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
            withTypeVHS[SierraTransformable, EmptyMetadata, Assertion](
              bucket,
              table) { hybridStore =>
              val oldBibRecord = createSierraBibRecordWith(
                modifiedDate = olderDate
              )

              val oldTransformable =
                SierraTransformable(bibRecord = oldBibRecord)

              val newBibRecord = createSierraBibRecordWith(
                id = oldBibRecord.id,
                modifiedDate = newerDate
              )

              storeInVHS(
                transformable = oldTransformable,
                hybridStore = hybridStore
              ).map { _ =>
                  sendNotificationToSQS(queue = queue, message = newBibRecord)
                }

              val expectedTransformable =
                SierraTransformable(bibRecord = newBibRecord)

              eventually {
                assertStored(
                  transformable = expectedTransformable,
                  bucket = bucket,
                  table = table
                )
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
            withTypeVHS[SierraTransformable, EmptyMetadata, Assertion](
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

              storeInVHS(
                transformable = expectedTransformable,
                hybridStore = hybridStore
              ).map { _ =>
                  sendNotificationToSQS(queue = queue, message = oldBibRecord)
                }

              // Blocking in Scala is generally a bad idea; we do it here so there's
              // enough time for this update to have gone through (if it was going to).
              Thread.sleep(5000)

              assertStored(
                transformable = expectedTransformable,
                bucket = bucket,
                table = table
              )
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
            withTypeVHS[SierraTransformable, EmptyMetadata, Unit](bucket, table) {
              hybridStore =>
                val transformable = createSierraTransformableWith(
                  maybeBibRecord = None
                )

                val bibRecord =
                  createSierraBibRecordWith(id = transformable.sierraId)

                storeInVHS(
                  transformable = transformable,
                  hybridStore = hybridStore
                ).map { _ =>
                    sendNotificationToSQS(queue = queue, message = bibRecord)
                  }

                val expectedTransformable =
                  SierraTransformable(bibRecord = bibRecord)

                eventually {
                  assertStored(
                    transformable = expectedTransformable,
                    bucket = bucket,
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
