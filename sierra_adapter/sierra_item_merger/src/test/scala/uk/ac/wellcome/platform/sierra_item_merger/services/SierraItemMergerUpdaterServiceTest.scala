package uk.ac.wellcome.platform.sierra_item_merger.services

import java.time.temporal.ChronoUnit

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec}
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.vhs.{SourceMetadata, VersionedHybridStore}
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SierraItemMergerUpdaterServiceTest
    extends FunSpec
    with ExtendedPatience
    with ScalaFutures
    with LocalVersionedHybridStore
    with SQS
    with SierraUtil {

  def withSierraUpdaterService(
    hybridStore: VersionedHybridStore[SierraTransformable,
                                      SourceMetadata,
                                      ObjectStore[SierraTransformable]])(
    testWith: TestWith[SierraItemMergerUpdaterService, Assertion]) = {
    val sierraUpdaterService = new SierraItemMergerUpdaterService(
      versionedHybridStore = hybridStore
    )
    testWith(sierraUpdaterService)
  }

  it("creates a record if it receives an item with a bibId that doesn't exist") {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
          bucket,
          table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val bibId = "b666"
            val newItemRecord = createSierraItemRecordWith(
              bibIds = List(bibId)
            )

            whenReady(sierraUpdaterService.update(newItemRecord)) { _ =>
              val expectedSierraTransformable =
                SierraTransformable(
                  sourceId = bibId,
                  maybeBibData = None,
                  itemData = Map(
                    newItemRecord.id -> newItemRecord
                  ))

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

  it("updates multiple merged records if the item contains multiple bibIds") {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
          bucket,
          table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val bibIdNotExisting = "b666"
            val bibIdWithOldData = "b555"
            val bibIdWithNewerData = "b777"

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
//
//            val itemRecord = sierraItemRecord(
//              id = itemId,
//              updatedDate = "2014-04-04T04:04:04Z",
//              bibIds = bibIds
//            )

//            val otherItem = sierraItemRecord(
//              id = "i888",
//              updatedDate = "2003-03-03T03:03:03Z",
//              bibIds = bibIds
//            )

            val oldRecord = SierraTransformable(
              sourceId = bibIdWithOldData,
              itemData = Map(
                itemRecord.id -> itemRecord.copy(
                  modifiedDate = itemRecord.modifiedDate.minus(1, ChronoUnit.HOURS)
                ),
                otherItemRecord.id -> otherItemRecord.copy(
                  modifiedDate = otherItemRecord.modifiedDate.minus(1, ChronoUnit.HOURS)
                )
              )
            )

            val f1 = hybridStore.updateRecord(oldRecord.id)(ifNotExisting =
              (oldRecord, SourceMetadata(oldRecord.sourceName)))(ifExisting =
              (t, m) => (t, m))

            val anotherItemRecord = createSierraItemRecordWith(
              bibIds = bibIds
            )

            val newRecord = SierraTransformable(
              sourceId = bibIdWithNewerData,
              itemData = Map(
                itemRecord.id -> itemRecord.copy(
                  modifiedDate = itemRecord.modifiedDate.minus(1, ChronoUnit.HOURS)
                ),
                anotherItemRecord.id -> anotherItemRecord.copy(
                  modifiedDate = anotherItemRecord.modifiedDate.plus(1, ChronoUnit.HOURS)
                )
              )
            )

            whenReady(f1) { _ =>
              val f2 = hybridStore.updateRecord(newRecord.id)(ifNotExisting =
                (newRecord, SourceMetadata(newRecord.sourceName)))(ifExisting =
                (t, m) => (t, m))

              whenReady(f2) { _ =>
                whenReady(sierraUpdaterService.update(itemRecord)) { _ =>
                  val expectedNewSierraTransformable =
                    SierraTransformable(
                      sourceId = bibIdNotExisting,
                      maybeBibData = None,
                      itemData = Map(itemRecord.id -> itemRecord)
                    )

                  assertStored[SierraTransformable](
                    bucket,
                    table,
                    expectedNewSierraTransformable)

                  val expectedUpdatedSierraTransformable = oldRecord.copy(
                    itemData = Map(
                      itemRecord.id -> itemRecord,
                      otherItemRecord.id -> otherItemRecord
                    )
                  )

                  assertStored[SierraTransformable](
                    bucket,
                    table,
                    expectedUpdatedSierraTransformable)
                  assertStored[SierraTransformable](bucket, table, newRecord)
                }
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
        withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
          bucket,
          table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val id = "i3000003"
            val bibId = "b3000003"

            val oldItemRecord = createSierraItemRecordWith(
              modifiedDate = olderDate,
              bibIds = List(bibId)
            )

            val oldRecord = SierraTransformable(
              sourceId = bibId,
              itemData = Map(oldItemRecord.id -> oldItemRecord)
            )

            val f1 = hybridStore.updateRecord(oldRecord.id)(ifNotExisting =
              (oldRecord, SourceMetadata(oldRecord.sourceName)))(ifExisting =
              (t, m) => (t, m))

            whenReady(f1) { _ =>
              val newItemRecord = createSierraItemRecordWith(
                id = oldItemRecord.id,
                modifiedDate = newerDate,
                bibIds = List(bibId)
              )

              whenReady(sierraUpdaterService.update(newItemRecord)) { _ =>
                val expectedSierraRecord = oldRecord.copy(
                  itemData = Map(id -> newItemRecord)
                )

                assertStored[SierraTransformable](
                  bucket,
                  table,
                  expectedSierraRecord)
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
        withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
          bucket,
          table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val bibId1 = "b9000001"
            val bibId2 = "b9000002"

            val itemRecord = createSierraItemRecordWith(
              bibIds = List(bibId1)
            )

            val itemData = Map(
              itemRecord.id -> itemRecord
            )

            val sierraTransformable1 = SierraTransformable(
              sourceId = bibId1,
              itemData = itemData
            )

            val sierraTransformable2 = SierraTransformable(
              sourceId = bibId2
            )

            val f1 = hybridStore.updateRecord(sierraTransformable1.id)(
              ifNotExisting = (
                sierraTransformable1,
                SourceMetadata(sierraTransformable1.sourceName)))(ifExisting =
              (t, m) => (t, m))

            val f2 = hybridStore.updateRecord(sierraTransformable2.id)(
              ifNotExisting = (
                sierraTransformable2,
                SourceMetadata(sierraTransformable2.sourceName)))(ifExisting =
              (t, m) => (t, m))

            val unlinkItemRecord = itemRecord.copy(
              bibIds = List(bibId2),
              unlinkedBibIds = List(bibId1),
              modifiedDate = itemRecord.modifiedDate.plusSeconds(1)
            )

            val unlinkItemRecordFuture = for {
              _ <- Future.sequence(List(f1, f2))
              _ <- sierraUpdaterService.update(unlinkItemRecord)
            } yield {}

            whenReady(unlinkItemRecordFuture) { _ =>
              val expectedSierraRecord1 = sierraTransformable1.copy(
                itemData = Map.empty
              )

              val expectedItemData = Map(
                itemRecord.id -> itemRecord.copy(
                  bibIds = List(bibId2),
                  unlinkedBibIds = List(bibId1),
                  modifiedDate = unlinkItemRecord.modifiedDate
                )
              )
              val expectedSierraRecord2 = sierraTransformable2.copy(
                itemData = expectedItemData
              )

              assertStored[SierraTransformable](
                bucket,
                table,
                expectedSierraRecord1)
              assertStored[SierraTransformable](
                bucket,
                table,
                expectedSierraRecord2)
            }
          }
        }
      }
    }
  }

  it("unlinks and updates a bib from a single call") {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
          bucket,
          table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val itemId = "i3000003"

            val bibId1 = "b9000001"
            val bibId2 = "b9000002"

            val itemRecord = createSierraItemRecordWith(
              bibIds = List(bibId1)
            )

            val itemData = Map(
              itemRecord.id -> itemRecord
            )

            val sierraTransformable1 = SierraTransformable(
              sourceId = bibId1,
              itemData = itemData
            )

            val sierraTransformable2 = SierraTransformable(
              sourceId = bibId2,
              itemData = itemData
            )

            val f1 = hybridStore.updateRecord(sierraTransformable1.id)(
              ifNotExisting = (
                sierraTransformable1,
                SourceMetadata(sierraTransformable1.sourceName)))(ifExisting =
              (t, m) => (t, m))

            val f2 = hybridStore.updateRecord(sierraTransformable2.id)(
              ifNotExisting = (
                sierraTransformable2,
                SourceMetadata(sierraTransformable2.sourceName)))(ifExisting =
              (t, m) => (t, m))

            val unlinkItemRecord = itemRecord.copy(
              bibIds = List(bibId2),
              unlinkedBibIds = List(bibId1),
              modifiedDate = itemRecord.modifiedDate.plusSeconds(1)
            )

            val expectedItemData = Map(
              itemRecord.id -> unlinkItemRecord
            )

            whenReady(Future.sequence(List(f1, f2))) { _ =>
              whenReady(sierraUpdaterService.update(unlinkItemRecord)) { _ =>
                val expectedSierraRecord1 = sierraTransformable1.copy(
                  itemData = Map.empty
                )

                // In this situation the item was already linked to sierraTransformable2
                // but the modified date is updated in line with the item update
                val expectedSierraRecord2 = sierraTransformable2.copy(
                  itemData = expectedItemData
                )

                assertStored[SierraTransformable](
                  bucket,
                  table,
                  expectedSierraRecord1)
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  expectedSierraRecord2)
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
        withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
          bucket,
          table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val bibId1 = "b9000001"
            val bibId2 = "b9000002"

            val itemRecord = createSierraItemRecordWith(
              bibIds = List(bibId1)
            )

            val sierraTransformable1 = SierraTransformable(
              sourceId = bibId1,
              itemData = Map(itemRecord.id -> itemRecord)
            )

            val sierraTransformable2 = SierraTransformable(
              sourceId = bibId2
            )

            val f1 = hybridStore.updateRecord(sierraTransformable1.id)(
              ifNotExisting = (
                sierraTransformable1,
                SourceMetadata(sierraTransformable1.sourceName)))(ifExisting =
              (t, m) => (t, m))

            val f2 = hybridStore.updateRecord(sierraTransformable2.id)(
              ifNotExisting = (
                sierraTransformable2,
                SourceMetadata(sierraTransformable2.sourceName)))(ifExisting =
              (t, m) => (t, m))

            val unlinkItemRecord = itemRecord.copy(
              bibIds = List(bibId2),
              unlinkedBibIds = List(bibId1),
              modifiedDate = itemRecord.modifiedDate.minusSeconds(1)
            )

            val expectedItemData = Map(
              itemRecord.id -> itemRecord.copy(
                bibIds = List(bibId2),
                unlinkedBibIds = List(bibId1),
                modifiedDate = unlinkItemRecord.modifiedDate
              )
            )

            whenReady(Future.sequence(List(f1, f2))) { _ =>
              whenReady(sierraUpdaterService.update(unlinkItemRecord)) { _ =>
                // In this situation the item will _not_ be unlinked from the original
                // record but will be linked to the new record (as this is the first
                // time we've seen the link so it is valid for that bib.
                val expectedSierraRecord1 = sierraTransformable1
                val expectedSierraRecord2 = sierraTransformable2.copy(
                  itemData = expectedItemData
                )

                assertStored[SierraTransformable](
                  bucket,
                  table,
                  expectedSierraRecord1)
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  expectedSierraRecord2)
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
        withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
          bucket,
          table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val bibId = "b6000006"

            val record = createSierraItemRecordWith(
              data = "<<newer data>>",
              modifiedDate = newerDate,
              bibIds = List(bibId)
            )

            val sierraRecord = SierraTransformable(
              sourceId = bibId,
              itemData = Map(record.id -> record)
            )

            val f1 = hybridStore.updateRecord(sierraRecord.id)(ifNotExisting =
              (sierraRecord, SourceMetadata(sierraRecord.sourceName)))(
              ifExisting = (t, m) => (t, m))

            val oldItemRecord = createSierraItemRecordWith(
              id = record.id,
              data = "<<older data>>",
              modifiedDate = olderDate,
              bibIds = record.bibIds
            )

            whenReady(f1) { _ =>
              whenReady(sierraUpdaterService.update(oldItemRecord)) { _ =>
                assertStored[SierraTransformable](bucket, table, sierraRecord)
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
        withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
          bucket,
          table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val bibId = "b7000007"

            val sierraRecord = SierraTransformable(
              sourceId = bibId
            )

            val f1 = hybridStore.updateRecord(sierraRecord.id)(ifNotExisting =
              (sierraRecord, SourceMetadata(sierraRecord.sourceName)))(
              ifExisting = (t, m) => (t, m))

            val itemRecord = createSierraItemRecordWith(
              bibIds = List(bibId)
            )

            whenReady(f1) { _ =>
              whenReady(sierraUpdaterService.update(itemRecord)) { _ =>
                val expectedSierraRecord = SierraTransformable(
                  sourceId = bibId,
                  itemData = Map(
                    itemRecord.id -> itemRecord
                  )
                )

                assertStored[SierraTransformable](
                  bucket,
                  table,
                  expectedSierraRecord)
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
      withTypeVHS[SierraTransformable, SourceMetadata, Assertion](bucket, table) {
        brokenStore =>
          withSierraUpdaterService(brokenStore) { brokenService =>
            val itemRecord = createSierraItemRecord

            whenReady(brokenService.update(itemRecord).failed) { ex =>
              ex shouldBe a[RuntimeException]
            }
          }
      }
    }
  }
}
