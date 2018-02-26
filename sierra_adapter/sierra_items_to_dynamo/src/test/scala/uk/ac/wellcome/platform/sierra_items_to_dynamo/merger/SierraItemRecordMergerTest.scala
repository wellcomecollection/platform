package uk.ac.wellcome.platform.sierra_items_to_dynamo.merger

import java.time.Instant

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.utils.JsonUtil._

class SierraItemRecordMergerTest extends FunSpec with Matchers {

  it("combines the bibIds in the final result") {
    val existingRecord =
      sierraItemRecord(
        bibIds = List("1", "2", "3"),
        modifiedDate = "2001-01-01T01:01:01Z")
    val updatedRecord =
      sierraItemRecord(
        bibIds = List("1", "2", "3", "4", "5"),
        modifiedDate = "2002-01-01T01:01:01Z")

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.bibIds shouldBe List("1", "2", "3", "4", "5")
    mergedRecord.unlinkedBibIds shouldBe List()
  }

  it("records unlinked bibIds") {
    val existingRecord =
      sierraItemRecord(
        bibIds = List("1", "2", "3"),
        modifiedDate = "2001-01-01T01:01:01Z")
    val updatedRecord = sierraItemRecord(
      bibIds = List("3", "4", "5"),
      modifiedDate = "2002-01-01T01:01:01Z")

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.bibIds shouldBe List("3", "4", "5")
    mergedRecord.unlinkedBibIds shouldBe List("1", "2")
  }

  it("preserves existing unlinked bibIds") {
    val existingRecord =
      sierraItemRecord(
        bibIds = List("1", "2", "3"),
        unlinkedBibIds = List("4", "5"),
        modifiedDate = "2001-01-01T01:01:01Z")
    val updatedRecord = sierraItemRecord(
      bibIds = List("1", "2", "3"),
      modifiedDate = "2002-01-01T01:01:01Z")

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.unlinkedBibIds shouldBe List("4", "5")
  }

  it("does not duplicate unlinked bibIds") {
    // This would be an unusual scenario to arise, but check we handle it anyway!
    val existingRecord =
      sierraItemRecord(
        bibIds = List("1", "2", "3"),
        unlinkedBibIds = List("3"),
        modifiedDate = "2001-01-01T01:01:01Z")
    val updatedRecord = sierraItemRecord(
      bibIds = List("1", "2"),
      modifiedDate = "2002-01-01T01:01:01Z")

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.bibIds shouldBe List("1", "2")
    mergedRecord.unlinkedBibIds shouldBe List("3")
  }

  it("removes an unlinked bibId if it appears on a new record") {
    val existingRecord =
      sierraItemRecord(
        bibIds = List(),
        unlinkedBibIds = List("1", "2", "3"),
        modifiedDate = "2001-01-01T01:01:01Z")
    val updatedRecord = sierraItemRecord(
      bibIds = List("1", "2"),
      modifiedDate = "2002-01-01T01:01:01Z")

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(
        existingRecord = existingRecord,
        updatedRecord = updatedRecord)
    mergedRecord.bibIds shouldBe List("1", "2")
    mergedRecord.unlinkedBibIds shouldBe List("3")
  }

  it(
    "should return the existing record unchanged if the update has an older date") {
    val existingRecord = sierraItemRecord(
      bibIds = List("1", "2", "3"),
      modifiedDate = "2017-01-01T00:00:00Z",
      version = 3
    )
    val updatedRecord = sierraItemRecord(
      bibIds = List("1", "2", "3", "4", "5"),
      modifiedDate = "2010-01-01T00:00:00Z"
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(existingRecord, updatedRecord)

    mergedRecord shouldBe existingRecord
  }

  it("carries across the version from the existing record") {
    val existingRecord = sierraItemRecord(
      modifiedDate = "2001-01-01T00:00:00Z",
      version = 10
    )
    val updatedRecord = sierraItemRecord(
      modifiedDate = "2009-09-09T00:00:00Z",
      version = 2
    )

    val mergedRecord =
      SierraItemRecordMerger.mergeItems(existingRecord, updatedRecord)
    mergedRecord.version shouldBe existingRecord.version
  }

  private def sierraItemRecord(bibIds: List[String] = List(),
                               unlinkedBibIds: List[String] = List(),
                               modifiedDate: String = "2001-01-01T01:01:01Z",
                               version: Int = 1): SierraItemRecord = {
    SierraItemRecord(
      id = s"i111",
      modifiedDate = Instant.parse(modifiedDate),
      data = s"""
           |{
           |  "id": "i111",
           |  "updatedDate": "$modifiedDate",
           |  "comment": "Legacy line of lamentable leopards",
           |  "bibIds": ${toJson(bibIds).get}
           |}""".stripMargin,
      bibIds = bibIds,
      unlinkedBibIds = unlinkedBibIds,
      version = version
    )
  }
}
