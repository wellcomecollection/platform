package uk.ac.wellcome.platform.sierra_item_merger.services

import java.time.Instant

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, FunSpec}
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.SierraTransformableCodec._
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
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
            val bibId = createSierraRecordNumber
            val newItemRecord = createSierraItemRecordWith(
              bibIds = List(bibId)
            )

            whenReady(sierraUpdaterService.update(newItemRecord)) { _ =>
              val expectedSierraTransformable =
                SierraTransformable(
                  sierraId = bibId,
                  maybeBibRecord = None,
                  itemRecords = Map(newItemRecord.id -> newItemRecord)
                )

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

  it("updates multiple merged records if the item contains multiple bibIds") {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
          bucket,
          table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val bibIdNotExisting = createSierraRecordNumber
            val bibIdWithOldData = createSierraRecordNumber
            val bibIdWithNewerData = createSierraRecordNumber

            val bibIds = List(
              bibIdNotExisting,
              bibIdWithOldData,
              bibIdWithNewerData
            )

            val itemRecord1 = createSierraItemRecordWith(
              modifiedDate = newerDate,
              bibIds = bibIds
            )

            val itemRecord2 = createSierraItemRecordWith(bibIds = bibIds)

            val oldTransformable = SierraTransformable(
              sierraId = bibIdWithOldData,
              itemRecords = Map(
                itemRecord1.id -> itemRecord1.copy(modifiedDate = olderDate),
                itemRecord2.id -> itemRecord2
              )
            )

            val f1 = hybridStore.updateRecord(oldTransformable.id)(
              ifNotExisting = (
                oldTransformable,
                SourceMetadata(oldTransformable.sourceName)))(ifExisting =
              (t, m) => (t, m))

            val anotherItem = createSierraItemRecordWith(bibIds = bibIds)

            val newTransformable = SierraTransformable(
              sierraId = bibIdWithNewerData,
              itemRecords = Map(
                itemRecord1.id -> itemRecord1.copy(modifiedDate = Instant.now),
                anotherItem.id -> anotherItem
              )
            )

            whenReady(f1) { _ =>
              val f2 = hybridStore.updateRecord(newTransformable.id)(
                ifNotExisting = (
                  newTransformable,
                  SourceMetadata(newTransformable.sourceName)))(ifExisting =
                (t, m) => (t, m))

              whenReady(f2) { _ =>
                whenReady(sierraUpdaterService.update(itemRecord1)) { _ =>
                  val expectedNewSierraTransformable =
                    SierraTransformable(
                      sierraId = bibIdNotExisting,
                      maybeBibRecord = None,
                      itemRecords = Map(itemRecord1.id -> itemRecord1)
                    )

                  assertStored[SierraTransformable](
                    bucket,
                    table,
                    id = expectedNewSierraTransformable.id,
                    record = expectedNewSierraTransformable)

                  val expectedUpdatedSierraTransformable =
                    oldTransformable.copy(
                      itemRecords = Map(
                        itemRecord1.id -> itemRecord1,
                        itemRecord2.id -> itemRecord2
                      )
                    )

                  assertStored[SierraTransformable](
                    bucket,
                    table,
                    id = expectedUpdatedSierraTransformable.id,
                    record = expectedUpdatedSierraTransformable)
                  assertStored[SierraTransformable](
                    bucket,
                    table,
                    id = newTransformable.id,
                    record = newTransformable)
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
            val bibId = createSierraRecordNumber
            val itemRecord = createSierraItemRecordWith(
              modifiedDate = olderDate,
              bibIds = List(bibId)
            )

            val oldTransformable = SierraTransformable(
              sierraId = bibId,
              itemRecords = Map(itemRecord.id -> itemRecord)
            )

            val f1 = hybridStore.updateRecord(oldTransformable.id)(
              ifNotExisting = (
                oldTransformable,
                SourceMetadata(oldTransformable.sourceName)))(ifExisting =
              (t, m) => (t, m))

            whenReady(f1) { _ =>
              val newItemRecord = itemRecord.copy(modifiedDate = newerDate)

              whenReady(sierraUpdaterService.update(newItemRecord)) { _ =>
                val expectedTransformable = oldTransformable.copy(
                  itemRecords = Map(newItemRecord.id -> newItemRecord)
                )

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
  }

  it("unlinks an item if it is updated with an unlinked item") {
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        withTypeVHS[SierraTransformable, SourceMetadata, Assertion](
          bucket,
          table) { hybridStore =>
          withSierraUpdaterService(hybridStore) { sierraUpdaterService =>
            val bibId1 = createSierraRecordNumber
            val bibId2 = createSierraRecordNumber

            val itemRecord = createSierraItemRecordWith(bibIds = List(bibId1))

            val itemData = Map(
              itemRecord.id -> itemRecord
            )

            val sierraTransformable1 = SierraTransformable(
              sierraId = bibId1,
              itemRecords = itemData
            )

            val sierraTransformable2 = SierraTransformable(
              sierraId = bibId2
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
                itemRecords = Map.empty
              )

              val expectedItemData = Map(
                itemRecord.id -> itemRecord.copy(
                  bibIds = List(bibId2),
                  unlinkedBibIds = List(bibId1),
                  modifiedDate = unlinkItemRecord.modifiedDate
                )
              )
              val expectedSierraRecord2 = sierraTransformable2.copy(
                itemRecords = expectedItemData
              )

              assertStored[SierraTransformable](
                bucket,
                table,
                id = expectedSierraRecord1.id,
                record = expectedSierraRecord1)
              assertStored[SierraTransformable](
                bucket,
                table,
                id = expectedSierraRecord2.id,
                record = expectedSierraRecord2)
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
            val bibId1 = createSierraRecordNumber
            val bibId2 = createSierraRecordNumber

            val itemRecord = createSierraItemRecordWith(bibIds = List(bibId1))

            val itemData = Map(
              itemRecord.id -> itemRecord
            )

            val sierraTransformable1 = SierraTransformable(
              sierraId = bibId1,
              itemRecords = itemData
            )

            val sierraTransformable2 = SierraTransformable(
              sierraId = bibId2,
              itemRecords = itemData
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
                  itemRecords = Map.empty
                )

                // In this situation the item was already linked to sierraTransformable2
                // but the modified date is updated in line with the item update
                val expectedSierraRecord2 = sierraTransformable2.copy(
                  itemRecords = expectedItemData
                )

                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = expectedSierraRecord1.id,
                  record = expectedSierraRecord1)
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = expectedSierraRecord2.id,
                  record = expectedSierraRecord2)
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
            val bibId1 = createSierraRecordNumber
            val bibId2 = createSierraRecordNumber

            val itemRecord = createSierraItemRecordWith(
              bibIds = List(bibId1)
            )

            val itemData = Map(
              itemRecord.id -> itemRecord
            )

            val sierraTransformable1 = SierraTransformable(
              sierraId = bibId1,
              itemRecords = itemData
            )

            val sierraTransformable2 = SierraTransformable(
              sierraId = bibId2
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
                  itemRecords = expectedItemData
                )

                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = expectedSierraRecord1.id,
                  record = expectedSierraRecord1)
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = expectedSierraRecord2.id,
                  record = expectedSierraRecord2)
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
            val bibId = createSierraRecordNumber

            val itemRecord = createSierraItemRecordWith(
              modifiedDate = newerDate,
              bibIds = List(bibId)
            )

            val sierraRecord = SierraTransformable(
              sierraId = bibId,
              itemRecords = Map(itemRecord.id -> itemRecord)
            )

            val f1 = hybridStore.updateRecord(sierraRecord.id)(ifNotExisting =
              (sierraRecord, SourceMetadata(sierraRecord.sourceName)))(
              ifExisting = (t, m) => (t, m))

            val oldItemRecord = itemRecord.copy(modifiedDate = olderDate)

            whenReady(f1) { _ =>
              whenReady(sierraUpdaterService.update(oldItemRecord)) { _ =>
                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = sierraRecord.id,
                  record = sierraRecord)
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
            val bibId = createSierraRecordNumber

            val sierraRecord = SierraTransformable(sierraId = bibId)

            val f1 = hybridStore.updateRecord(sierraRecord.id)(ifNotExisting =
              (sierraRecord, SourceMetadata(sierraRecord.sourceName)))(
              ifExisting = (t, m) => (t, m))

            val itemRecord = createSierraItemRecordWith(
              bibIds = List(bibId)
            )

            whenReady(f1) { _ =>
              whenReady(sierraUpdaterService.update(itemRecord)) { _ =>
                val expectedSierraRecord = SierraTransformable(
                  sierraId = bibId,
                  itemRecords = Map(itemRecord.id -> itemRecord)
                )

                assertStored[SierraTransformable](
                  bucket,
                  table,
                  id = expectedSierraRecord.id,
                  record = expectedSierraRecord)
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
