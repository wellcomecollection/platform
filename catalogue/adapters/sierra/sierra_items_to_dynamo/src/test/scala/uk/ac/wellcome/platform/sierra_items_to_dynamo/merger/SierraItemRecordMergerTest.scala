package uk.ac.wellcome.platform.sierra_items_to_dynamo.merger

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators

class SierraItemRecordMergerTest
    extends FunSpec
    with Matchers
    with SierraGenerators {

  it("combines the bibIds in the final result") {
    val bibIds = createSierraBibNumbers(count = 5)
    val existingRecord = createSierraItemRecordWith(
      modifiedDate = olderDate,
      bibIds = bibIds.slice(0, 3)
    )
    val updatedRecord = createSierraItemRecordWith(
      id = existingRecord.id,
      modifiedDate = newerDate,
      bibIds = bibIds
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.bibIds shouldBe bibIds
    mergedRecord.unlinkedBibIds shouldBe List()
  }

  it("records unlinked bibIds") {
    val bibIds = createSierraBibNumbers(count = 5)

    val existingRecord = createSierraItemRecordWith(
      modifiedDate = olderDate,
      bibIds = bibIds.slice(0, 3)
    )
    val updatedRecord = createSierraItemRecordWith(
      id = existingRecord.id,
      modifiedDate = newerDate,
      bibIds = bibIds.slice(2, 5)
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.bibIds shouldBe bibIds.slice(2, 5)
    mergedRecord.unlinkedBibIds shouldBe bibIds.slice(0, 2)
  }

  it("preserves existing unlinked bibIds") {
    val bibIds = createSierraBibNumbers(count = 5)

    val existingRecord = createSierraItemRecordWith(
      modifiedDate = olderDate,
      bibIds = bibIds.slice(0, 3),
      unlinkedBibIds = bibIds.slice(3, 5)
    )
    val updatedRecord = createSierraItemRecordWith(
      id = existingRecord.id,
      modifiedDate = newerDate,
      bibIds = existingRecord.bibIds
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.unlinkedBibIds should contain theSameElementsAs existingRecord.unlinkedBibIds
  }

  it("does not duplicate unlinked bibIds") {
    // This would be an unusual scenario to arise, but check we handle it anyway!
    val bibIds = createSierraBibNumbers(count = 3)

    val existingRecord = createSierraItemRecordWith(
      modifiedDate = olderDate,
      bibIds = bibIds,
      unlinkedBibIds = List(bibIds(2))
    )
    val updatedRecord = createSierraItemRecordWith(
      id = existingRecord.id,
      modifiedDate = newerDate,
      bibIds = bibIds.slice(0, 2)
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.bibIds shouldBe bibIds.slice(0, 2)
    mergedRecord.unlinkedBibIds shouldBe List(bibIds(2))
  }

  it("removes an unlinked bibId if it appears on a new record") {
    val bibIds = createSierraBibNumbers(count = 3)

    val existingRecord = createSierraItemRecordWith(
      modifiedDate = olderDate,
      bibIds = List(),
      unlinkedBibIds = bibIds
    )
    val updatedRecord = createSierraItemRecordWith(
      id = existingRecord.id,
      modifiedDate = newerDate,
      bibIds = bibIds.slice(0, 2)
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.bibIds shouldBe bibIds.slice(0, 2)
    mergedRecord.unlinkedBibIds shouldBe List(bibIds(2))
  }

  it("returns the existing record unchanged if the update has an older date") {
    val bibIds = createSierraBibNumbers(count = 5)

    val existingRecord = createSierraItemRecordWith(
      modifiedDate = newerDate,
      bibIds = bibIds.slice(0, 3)
    )
    val updatedRecord = createSierraItemRecordWith(
      id = existingRecord.id,
      modifiedDate = newerDate,
      bibIds = bibIds
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(existingRecord, updatedRecord)

    mergedRecord shouldBe existingRecord
  }
}
