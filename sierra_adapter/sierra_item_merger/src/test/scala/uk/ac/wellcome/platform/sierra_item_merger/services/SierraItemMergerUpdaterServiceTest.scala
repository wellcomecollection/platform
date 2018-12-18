package uk.ac.wellcome.platform.sierra_item_merger.services

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators
import uk.ac.wellcome.platform.sierra_item_merger.fixtures.SierraItemMergerFixtures
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.sierra_adapter.utils.SierraAdapterHelpers

class SierraItemMergerUpdaterServiceTest
    extends FunSpec
    with IntegrationPatience
    with ScalaFutures
    with LocalVersionedHybridStore
    with SQS
    with SierraAdapterHelpers
    with SierraGenerators
    with SierraItemMergerFixtures {

  it("creates a record if it receives an item with a bibId that doesn't exist") {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withSierraVHS(bucket, table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val bibId = createSierraBibNumber
            val newItemRecord = createSierraItemRecordWith(
              bibIds = List(bibId)
            )

            whenReady(sierraUpdaterService.update(newItemRecord)) { _ =>
              val expectedSierraTransformable =
                createSierraTransformableWith(
                  sierraId = bibId,
                  maybeBibRecord = None,
                  itemRecords = List(newItemRecord)
                )

              assertStored(
                transformable = expectedSierraTransformable,
                table = table
              )
            }
          }
        }
      }
    }
  }

  it("updates multiple merged records if the item contains multiple bibIds") {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withSierraVHS(bucket, table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val bibIdNotExisting = createSierraBibNumber
            val bibIdWithOldData = createSierraBibNumber
            val bibIdWithNewerData = createSierraBibNumber

            val bibIds = List(
              bibIdNotExisting,
              bibIdWithOldData,
              bibIdWithNewerData
            )

            val itemRecord = createSierraItemRecordWith(
              bibIds = bibIds
            )

            val otherItemRecord = createSierraItemRecordWith(
              bibIds = bibIds
            )

            val oldTransformable = createSierraTransformableWith(
              sierraId = bibIdWithOldData,
              maybeBibRecord = None,
              itemRecords = List(itemRecord, otherItemRecord)
            )

            val anotherItemRecord = createSierraItemRecordWith(
              bibIds = bibIds
            )

            val newTransformable = createSierraTransformableWith(
              sierraId = bibIdWithNewerData,
              maybeBibRecord = None,
              itemRecords = List(itemRecord, anotherItemRecord)
            )

            val expectedNewSierraTransformable =
              createSierraTransformableWith(
                sierraId = bibIdNotExisting,
                maybeBibRecord = None,
                itemRecords = List(itemRecord)
              )

            val expectedUpdatedSierraTransformable =
              createSierraTransformableWith(
                sierraId = oldTransformable.sierraId,
                maybeBibRecord = None,
                itemRecords = List(itemRecord, otherItemRecord)
              )

            val future = storeInVHS(
              transformables = List(oldTransformable, newTransformable),
              hybridStore = hybridStore
            )

            whenReady(future) { _ =>
              whenReady(sierraUpdaterService.update(itemRecord)) { _ =>
                assertStored(
                  transformable = expectedNewSierraTransformable,
                  table = table
                )
                assertStored(
                  transformable = expectedUpdatedSierraTransformable,
                  table = table
                )
                assertStored(
                  transformable = newTransformable,
                  table = table
                )
              }
            }
          }
        }
      }
    }
  }

  it("updates an item if it receives an update with a newer date") {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withSierraVHS(bucket, table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val bibId = createSierraBibNumber

            val itemRecord = createSierraItemRecordWith(
              modifiedDate = olderDate,
              bibIds = List(bibId)
            )

            val oldTransformable = createSierraTransformableWith(
              sierraId = bibId,
              maybeBibRecord = None,
              itemRecords = List(itemRecord)
            )

            val future = storeInVHS(
              transformable = oldTransformable,
              hybridStore = hybridStore
            )

            whenReady(future) { _ =>
              val newItemRecord = itemRecord.copy(
                data = """{"data": "newer"}""",
                modifiedDate = newerDate
              )

              whenReady(sierraUpdaterService.update(newItemRecord)) { _ =>
                val expectedTransformable = oldTransformable.copy(
                  itemRecords = Map(itemRecord.id -> newItemRecord)
                )

                assertStored(
                  transformable = expectedTransformable,
                  table = table
                )
              }
            }
          }
        }
      }
    }
  }

  it("unlinks an item if it is updated with an unlinked item") {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withSierraVHS(bucket, table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val bibId1 = createSierraBibNumber
            val bibId2 = createSierraBibNumber

            val itemRecord = createSierraItemRecordWith(
              bibIds = List(bibId1)
            )

            val sierraTransformable1 = createSierraTransformableWith(
              sierraId = bibId1,
              maybeBibRecord = None,
              itemRecords = List(itemRecord)
            )

            val sierraTransformable2 = createSierraTransformableWith(
              sierraId = bibId2,
              maybeBibRecord = None
            )

            val unlinkItemRecord = itemRecord.copy(
              bibIds = List(bibId2),
              unlinkedBibIds = List(bibId1),
              modifiedDate = itemRecord.modifiedDate.plusSeconds(1)
            )

            val expectedTransformable1 = sierraTransformable1.copy(
              itemRecords = Map.empty
            )

            val expectedItemRecords = Map(
              itemRecord.id -> itemRecord.copy(
                bibIds = List(bibId2),
                unlinkedBibIds = List(bibId1),
                modifiedDate = unlinkItemRecord.modifiedDate
              )
            )
            val expectedTransformable2 = sierraTransformable2.copy(
              itemRecords = expectedItemRecords
            )

            val future = storeInVHS(
              transformables = List(sierraTransformable1, sierraTransformable2),
              hybridStore = hybridStore
            )

            whenReady(future) { _ =>
              whenReady(sierraUpdaterService.update(unlinkItemRecord)) { _ =>
                assertStored(
                  transformable = expectedTransformable1,
                  table = table
                )
                assertStored(
                  transformable = expectedTransformable2,
                  table = table
                )
              }
            }
          }
        }
      }
    }
  }

  it("unlinks and updates a bib from a single call") {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withSierraVHS(bucket, table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val bibId1 = createSierraBibNumber
            val bibId2 = createSierraBibNumber

            val itemRecord = createSierraItemRecordWith(
              bibIds = List(bibId1)
            )

            val sierraTransformable1 = createSierraTransformableWith(
              sierraId = bibId1,
              maybeBibRecord = None,
              itemRecords = List(itemRecord)
            )

            val sierraTransformable2 = createSierraTransformableWith(
              sierraId = bibId2,
              maybeBibRecord = None,
              itemRecords = List(itemRecord)
            )

            val unlinkItemRecord = itemRecord.copy(
              bibIds = List(bibId2),
              unlinkedBibIds = List(bibId1),
              modifiedDate = itemRecord.modifiedDate.plusSeconds(1)
            )

            val expectedItemData = Map(
              itemRecord.id -> unlinkItemRecord
            )

            val expectedTransformable1 = sierraTransformable1.copy(
              itemRecords = Map.empty
            )

            // In this situation the item was already linked to sierraTransformable2
            // but the modified date is updated in line with the item update
            val expectedTransformable2 = sierraTransformable2.copy(
              itemRecords = expectedItemData
            )

            val future = storeInVHS(
              transformables = List(sierraTransformable1, sierraTransformable2),
              hybridStore = hybridStore
            )

            whenReady(future) { _ =>
              whenReady(sierraUpdaterService.update(unlinkItemRecord)) { _ =>
                assertStored(
                  transformable = expectedTransformable1,
                  table = table
                )
                assertStored(
                  transformable = expectedTransformable2,
                  table = table
                )
              }
            }
          }
        }
      }
    }
  }

  it("does not unlink an item if it receives an out of date unlink update") {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withSierraVHS(bucket, table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val bibId1 = createSierraBibNumber
            val bibId2 = createSierraBibNumber

            val itemRecord = createSierraItemRecordWith(
              bibIds = List(bibId1)
            )

            val sierraTransformable1 = createSierraTransformableWith(
              sierraId = bibId1,
              maybeBibRecord = None,
              itemRecords = List(itemRecord)
            )

            val sierraTransformable2 = createSierraTransformableWith(
              sierraId = bibId2,
              maybeBibRecord = None
            )

            val unlinkItemRecord = itemRecord.copy(
              bibIds = List(bibId2),
              unlinkedBibIds = List(bibId1),
              modifiedDate = itemRecord.modifiedDate.minusSeconds(1)
            )

            val expectedItemRecords = Map(
              itemRecord.id -> itemRecord.copy(
                bibIds = List(bibId2),
                unlinkedBibIds = List(bibId1),
                modifiedDate = unlinkItemRecord.modifiedDate
              )
            )

            // In this situation the item will _not_ be unlinked from the original
            // record but will be linked to the new record (as this is the first
            // time we've seen the link so it is valid for that bib.
            val expectedTransformable1 = sierraTransformable1
            val expectedTransformable2 = sierraTransformable2.copy(
              itemRecords = expectedItemRecords
            )

            val future = storeInVHS(
              transformables = List(sierraTransformable1, sierraTransformable2),
              hybridStore = hybridStore
            )

            whenReady(future) { _ =>
              whenReady(sierraUpdaterService.update(unlinkItemRecord)) { _ =>
                assertStored(
                  transformable = expectedTransformable1,
                  table = table
                )
                assertStored(
                  transformable = expectedTransformable2,
                  table = table
                )
              }
            }
          }
        }
      }
    }
  }

  it("does not update an item if it receives an update with an older date") {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withSierraVHS(bucket, table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val bibId = createSierraBibNumber

            val itemRecord = createSierraItemRecordWith(
              modifiedDate = newerDate,
              bibIds = List(bibId)
            )

            val transformable = createSierraTransformableWith(
              sierraId = bibId,
              maybeBibRecord = None,
              itemRecords = List(itemRecord)
            )

            val oldItemRecord = itemRecord.copy(
              modifiedDate = olderDate
            )

            val future = storeInVHS(
              transformable = transformable,
              hybridStore = hybridStore
            )

            whenReady(future) { _ =>
              whenReady(sierraUpdaterService.update(oldItemRecord)) { _ =>
                assertStored(
                  transformable = transformable,
                  table = table
                )
              }
            }
          }
        }
      }
    }
  }

  it("adds an item to the record if the bibId exists but has no itemData") {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withSierraVHS(bucket, table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val bibId = createSierraBibNumber
            val transformable = createSierraTransformableWith(
              sierraId = bibId,
              maybeBibRecord = None
            )

            val itemRecord = createSierraItemRecordWith(
              bibIds = List(bibId)
            )

            val expectedTransformable = createSierraTransformableWith(
              sierraId = bibId,
              maybeBibRecord = None,
              itemRecords = List(itemRecord)
            )

            val future = storeInVHS(
              transformable = transformable,
              hybridStore = hybridStore
            )

            whenReady(future) { _ =>
              whenReady(sierraUpdaterService.update(itemRecord)) { _ =>
                assertStored(
                  transformable = expectedTransformable,
                  table = table
                )
              }
            }
          }
        }
      }
    }
  }

  it("returns a failed future if putting an item fails") {
    withLocalS3Bucket { bucket =>
      val table = Table(name = "doesnotexist", index = "missing")
      withSierraVHS(bucket, table) { brokenStore =>
        withSierraUpdaterService(brokenStore) { brokenService =>
          val itemRecord = createSierraItemRecordWith(
            bibIds = List(createSierraBibNumber)
          )

          whenReady(brokenService.update(itemRecord).failed) { ex =>
            ex shouldBe a[RuntimeException]
          }
        }
      }
    }
  }
}
