package uk.ac.wellcome.platform.sierra_items_to_dynamo.merger

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil

class SierraItemRecordMergerTest extends FunSpec with Matchers with SierraUtil {

  it("combines the bibIds in the final result") {
    val existingRecord = createSierraItemRecordWith(
      bibIds = List("1", "2", "3"),
      modifiedDate = olderDate
    )
    val updatedRecord = createSierraItemRecordWith(
      id = existingRecord.id,
      bibIds = List("1", "2", "3", "4", "5"),
      modifiedDate = newerDate
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.bibIds shouldBe List("1", "2", "3", "4", "5")
    mergedRecord.unlinkedBibIds shouldBe List()
  }

  it("records unlinked bibIds") {
    val existingRecord = createSierraItemRecordWith(
      bibIds = List("1", "2", "3"),
      modifiedDate = olderDate
    )
    val updatedRecord = createSierraItemRecordWith(
      id = existingRecord.id,
      bibIds = List("3", "4", "5"),
      modifiedDate = newerDate
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.bibIds shouldBe List("3", "4", "5")
    mergedRecord.unlinkedBibIds shouldBe List("1", "2")
  }

  it("preserves existing unlinked bibIds") {
    val existingRecord = createSierraItemRecordWith(
      bibIds = List("1", "2", "3"),
      unlinkedBibIds = List("4", "5"),
      modifiedDate = olderDate
    )
    val updatedRecord = createSierraItemRecordWith(
      id = existingRecord.id,
      bibIds = List("1", "2", "3"),
      modifiedDate = newerDate
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.unlinkedBibIds shouldBe List("4", "5")
  }

  it("does not duplicate unlinked bibIds") {
    // This would be an unusual scenario to arise, but check we handle it anyway!
    val existingRecord = createSierraItemRecordWith(
      bibIds = List("1", "2", "3"),
      unlinkedBibIds = List("3"),
      modifiedDate = olderDate
    )
    val updatedRecord = createSierraItemRecordWith(
      id = existingRecord.id,
      bibIds = List("1", "2"),
      modifiedDate = newerDate
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.bibIds shouldBe List("1", "2")
    mergedRecord.unlinkedBibIds shouldBe List("3")
  }

  it("removes an unlinked bibId if it appears on a new record") {
    val existingRecord = createSierraItemRecordWith(
      bibIds = List(),
      unlinkedBibIds = List("1", "2", "3"),
      modifiedDate = olderDate
    )
    val updatedRecord = createSierraItemRecordWith(
      id = existingRecord.id,
      bibIds = List("1", "2"),
      modifiedDate = newerDate
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.bibIds shouldBe List("1", "2")
    mergedRecord.unlinkedBibIds shouldBe List("3")
  }

  it("returns the existing record unchanged if the update has an older date") {
    val existingRecord = createSierraItemRecordWith(
      bibIds = List("1", "2", "3"),
      modifiedDate = newerDate,
      version = 3
    )
    val updatedRecord = createSierraItemRecordWith(
      id = existingRecord.id,
      bibIds = List("1", "2", "3", "4", "5"),
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
    val updatedRecord = createSierraItemRecordWith(
      id = existingRecord.id,
      modifiedDate = newerDate,
      version = 2
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(existingRecord, updatedRecord)
    mergedRecord.version shouldBe existingRecord.version
  }
}
