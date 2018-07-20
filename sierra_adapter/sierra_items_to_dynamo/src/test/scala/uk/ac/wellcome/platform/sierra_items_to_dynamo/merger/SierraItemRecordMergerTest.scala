package uk.ac.wellcome.platform.sierra_items_to_dynamo.merger

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil

class SierraItemRecordMergerTest extends FunSpec with Matchers with SierraUtil {

  it("combines the bibIds in the final result") {
    val bibIds = (1 to 3).map { _ => createSierraRecordNumber }.toList
    val extraBibIds = (4 to 5).map { _ => createSierraRecordNumber }.toList

    val existingRecord = createSierraItemRecordWith(
      bibIds = bibIds,
      modifiedDate = olderDate
    )
    val updatedRecord = existingRecord.copy(
      bibIds = bibIds ++ extraBibIds,
      modifiedDate = newerDate
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.bibIds shouldBe bibIds ++ extraBibIds
    mergedRecord.unlinkedBibIds shouldBe List()
  }

  it("records unlinked bibIds") {
    val oldbibIds = (1 to 2).map { _ => createSierraRecordNumber }.toList
    val commonBibId = List(createSierraRecordNumber)
    val newBibIds = (4 to 5).map { _ => createSierraRecordNumber }.toList

    val existingRecord = createSierraItemRecordWith(
      bibIds = oldbibIds ++ commonBibId,
      modifiedDate = olderDate
    )
    val updatedRecord = existingRecord.copy(
      bibIds = commonBibId ++ newBibIds,
      modifiedDate = newerDate
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.bibIds shouldBe (commonBibId ++ newBibIds)
    mergedRecord.unlinkedBibIds shouldBe oldbibIds
  }

  it("preserves existing unlinked bibIds") {
    val bibIds = (1 to 3).map { _ => createSierraRecordNumber }.toList
    val unlinkedBibIds = (4 to 5).map { _ => createSierraRecordNumber }.toList

    val existingRecord = createSierraItemRecordWith(
      bibIds = bibIds,
      unlinkedBibIds = unlinkedBibIds,
      modifiedDate = olderDate
    )
    val updatedRecord = existingRecord.copy(
      unlinkedBibIds = List(),
      modifiedDate = newerDate
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.unlinkedBibIds shouldBe unlinkedBibIds
  }

  it("does not duplicate unlinked bibIds") {
    val bibIds = (1 to 3).map { _ => createSierraRecordNumber }.toList

    // This would be an unusual scenario to arise, but check we handle it anyway!
    val existingRecord = createSierraItemRecordWith(
      bibIds = bibIds,
      unlinkedBibIds = List(bibIds.head),
      modifiedDate = olderDate
    )
    val updatedRecord = existingRecord.copy(
      bibIds = bibIds.tail,
      modifiedDate = newerDate
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.bibIds shouldBe bibIds.tail
    mergedRecord.unlinkedBibIds shouldBe List(bibIds.head)
  }

  it("removes an unlinked bibId if it appears on a new record") {
    val bibIds = (1 to 2).map { _ => createSierraRecordNumber }.toList
    val unlinkedBibId = List(createSierraRecordNumber)

    val existingRecord = createSierraItemRecordWith(
      unlinkedBibIds = bibIds ++ unlinkedBibId,
      modifiedDate = olderDate
    )
    val updatedRecord = existingRecord.copy(
      bibIds = bibIds,
      unlinkedBibIds = List(),
      modifiedDate = newerDate
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.bibIds shouldBe bibIds
    mergedRecord.unlinkedBibIds shouldBe unlinkedBibId
  }

  it("returns the existing record unchanged if the update has an older date") {
    val bibIds = (1 to 3).map { _ => createSierraRecordNumber }.toList
    val extraBibIds = (4 to 5).map { _ => createSierraRecordNumber }.toList

    val existingRecord = createSierraItemRecordWith(
      bibIds = bibIds,
      modifiedDate = newerDate
    )
    val updatedRecord = existingRecord.copy(
      bibIds = bibIds ++ extraBibIds,
      modifiedDate = olderDate
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(existingRecord, updatedRecord)

    mergedRecord shouldBe existingRecord
  }

  it("carries across the version from the existing record") {
    val existingRecord = createSierraItemRecordWith(
      modifiedDate = olderDate,
      version = 10
    )
    val updatedRecord = existingRecord.copy(
      modifiedDate = newerDate,
      version = 2
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(existingRecord, updatedRecord)
    mergedRecord.version shouldBe existingRecord.version
  }
}
