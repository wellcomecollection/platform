package uk.ac.wellcome.platform.sierra_item_merger

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators
import uk.ac.wellcome.storage.fixtures.{LocalVersionedHybridStore, S3}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.sierra_adapter.utils.SierraAdapterHelpers

class SierraItemMergerFeatureTest
    extends FunSpec
    with Matchers
    with Eventually
    with IntegrationPatience
    with fixtures.Server
    with SQS
    with S3
    with LocalVersionedHybridStore
    with SierraGenerators
    with SierraAdapterHelpers {

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
                withSierraVHS(sierraDataBucket, table) { hybridStore =>
                  val bibId = createSierraBibNumber

                  val itemRecord = createSierraItemRecordWith(
                    bibIds = List(bibId)
                  )

                  sendMessage(
                    bucket = sierraItemsToDynamoBucket,
                    queue = queue,
                    itemRecord
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
                withSierraVHS(sierraDataBucket, table) { _ =>
                  val bibId1 = createSierraBibNumber
                  val itemRecord1 = createSierraItemRecordWith(
                    bibIds = List(bibId1)
                  )

                  sendMessage(
                    bucket = sierraItemsToDynamoBucket,
                    queue = queue,
                    itemRecord1
                  )

                  val bibId2 = createSierraBibNumber
                  val itemRecord2 = createSierraItemRecordWith(
                    bibIds = List(bibId2)
                  )

                  sendMessage(
                    bucket = sierraItemsToDynamoBucket,
                    queue = queue,
                    itemRecord2
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
                withSierraVHS(sierraDataBucket, table) { _ =>
                  val bibIds = createSierraBibNumbers(3)
                  val itemRecord = createSierraItemRecordWith(
                    bibIds = bibIds
                  )

                  sendMessage(
                    bucket = sierraItemsToDynamoBucket,
                    queue = queue,
                    itemRecord
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
}
