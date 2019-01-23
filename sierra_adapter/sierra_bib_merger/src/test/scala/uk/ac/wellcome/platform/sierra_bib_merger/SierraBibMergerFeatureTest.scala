package uk.ac.wellcome.platform.sierra_bib_merger

import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators
import uk.ac.wellcome.platform.sierra_bib_merger.fixtures.WorkerServiceFixture
import uk.ac.wellcome.sierra_adapter.utils.SierraAdapterHelpers
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore

import scala.concurrent.ExecutionContext.Implicits.global

class SierraBibMergerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with MockitoSugar
    with IntegrationPatience
    with ScalaFutures
    with SQS
    with LocalVersionedHybridStore
    with SierraGenerators
    with SierraAdapterHelpers
    with WorkerServiceFixture {

  it("stores a bib in the hybrid store") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withLocalDynamoDbTable { table =>
            withWorkerService(bucket, table, queue, topic) { _ =>
              val bibRecord = createSierraBibRecord

              sendNotificationToSQS(queue = queue, message = bibRecord)

              val expectedSierraTransformable =
                SierraTransformable(bibRecord = bibRecord)

              eventually {
                assertStoredAndSent(
                  transformable = expectedSierraTransformable,
                  topic = topic,
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
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withLocalDynamoDbTable { table =>
            withWorkerService(bucket, table, queue, topic) { _ =>
              val record1 = createSierraBibRecord
              sendNotificationToSQS(queue = queue, message = record1)

              val expectedTransformable1 =
                SierraTransformable(bibRecord = record1)

              val record2 = createSierraBibRecord

              sendNotificationToSQS(queue = queue, message = record2)

              val expectedTransformable2 =
                SierraTransformable(bibRecord = record2)

              eventually {
                assertStoredAndSent(
                  transformable = expectedTransformable1,
                  topic = topic,
                  table = table
                )
                assertStoredAndSent(
                  transformable = expectedTransformable2,
                  topic = topic,
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
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withLocalDynamoDbTable { table =>
            withWorkerService(bucket, table, queue, topic) { _ =>
              withSierraVHS(bucket, table) { hybridStore =>
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
                  assertStoredAndSent(
                    transformable = expectedTransformable,
                    topic = topic,
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

  it("does not update a bib if an older version is sent to SQS") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withLocalDynamoDbTable { table =>
            withWorkerService(bucket, table, queue, topic) { _ =>
              withSierraVHS(bucket, table) { hybridStore =>
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

                assertStoredAndSent(
                  transformable = expectedTransformable,
                  topic = topic,
                  table = table
                )
              }
            }
          }
        }
      }
    }
  }

  it("stores a bib from SQS if the ID already exists but no bibData") {
    withLocalSqsQueue { queue =>
      withLocalSnsTopic { topic =>
        withLocalS3Bucket { bucket =>
          withLocalDynamoDbTable { table =>
            withWorkerService(bucket, table, queue, topic) { _ =>
              withSierraVHS(bucket, table) { hybridStore =>
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
                  assertStoredAndSent(
                    transformable = expectedTransformable,
                    topic = topic,
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
