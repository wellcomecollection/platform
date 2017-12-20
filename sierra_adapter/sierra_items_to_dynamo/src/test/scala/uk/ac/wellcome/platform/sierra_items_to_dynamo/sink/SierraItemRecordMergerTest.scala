package uk.ac.wellcome.platform.sierra_items_to_dynamo.sink

import io.circe.optics.JsonPath.root
import io.circe.parser._
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.SierraItemRecord
import uk.ac.wellcome.utils.JsonUtil

class SierraItemRecordMergerTest extends FunSpec with Matchers {

  it("combines the bibIds in the final result") {
    val oldRecord = sierraItemRecord(bibIds = List("1", "2", "3"), modifiedDate = "2001-01-01T01:01:01Z")
    val newRecord = sierraItemRecord(bibIds = List("1", "2", "3", "4", "5"), modifiedDate = "2002-01-01T01:01:01Z")

    val mergedRecord = SierraItemRecordMerger.mergeItems(oldRecord = oldRecord,
                                                         newRecord = newRecord)
    mergedRecord.bibIds shouldBe List("1", "2", "3", "4", "5")
    mergedRecord.unlinkedBibIds shouldBe List()
  }

  it("records unlinked bibIds") {
    val oldRecord = sierraItemRecord(bibIds = List("1", "2", "3"), modifiedDate = "2001-01-01T01:01:01Z")
    val newRecord = sierraItemRecord(bibIds = List("3", "4", "5"), modifiedDate = "2002-01-01T01:01:01Z")

    val mergedRecord = SierraItemRecordMerger.mergeItems(oldRecord = oldRecord,
                                                         newRecord = newRecord)
    mergedRecord.bibIds shouldBe List("3", "4", "5")
    mergedRecord.unlinkedBibIds shouldBe List("1", "2")
  }

  it("preserves existing unlinked bibIds") {
    val oldRecord = sierraItemRecord(bibIds = List("1", "2", "3"),
                                     unlinkedBibIds = List("4", "5"), modifiedDate = "2001-01-01T01:01:01Z")
    val newRecord = sierraItemRecord(bibIds = List("1", "2", "3"), modifiedDate = "2002-01-01T01:01:01Z")

    val mergedRecord = SierraItemRecordMerger.mergeItems(oldRecord = oldRecord,
                                                         newRecord = newRecord)
    mergedRecord.unlinkedBibIds shouldBe List("4", "5")
  }

  it("does not duplicate unlinked bibIds") {
    // This would be an unusual scenario to arise, but check we handle it anyway!
    val oldRecord = sierraItemRecord(bibIds = List("1", "2", "3"),
                                     unlinkedBibIds = List("3"), modifiedDate = "2001-01-01T01:01:01Z")
    val newRecord = sierraItemRecord(bibIds = List("1", "2"), modifiedDate = "2002-01-01T01:01:01Z")

    val mergedRecord = SierraItemRecordMerger.mergeItems(oldRecord = oldRecord,
                                                         newRecord = newRecord)
    mergedRecord.bibIds shouldBe List("1", "2")
    mergedRecord.unlinkedBibIds shouldBe List("3")
  }

  it("removes an unlinked bibId if it appears on a new record") {
    val oldRecord =
      sierraItemRecord(bibIds = List(), unlinkedBibIds = List("1", "2", "3"), modifiedDate = "2001-01-01T01:01:01Z")
    val newRecord = sierraItemRecord(bibIds = List("1", "2"), modifiedDate = "2002-01-01T01:01:01Z")

    val mergedRecord = SierraItemRecordMerger.mergeItems(oldRecord = oldRecord,
                                                         newRecord = newRecord)
    mergedRecord.bibIds shouldBe List("1", "2")
    mergedRecord.unlinkedBibIds shouldBe List("3")
  }

  it("should return the oldRecord unchanged if the newRecord has an older date") {
    val oldRecord = sierraItemRecord(bibIds = List("1", "2", "3"), modifiedDate = "2017-01-01T00:00:00Z")
    val newRecord = sierraItemRecord(bibIds = List("1", "2", "3", "4", "5"), modifiedDate = "2010-01-01T00:00:00Z")
    
    val mergedRecord = SierraItemRecordMerger.mergeItems(oldRecord, newRecord)
    
    mergedRecord shouldBe oldRecord
  }

  private def sierraItemRecord(
    bibIds: List[String],
    unlinkedBibIds: List[String] = List(), modifiedDate: String = "2001-01-01T01:01:01Z"): SierraItemRecord = {
    SierraItemRecord(
      id = s"i111",
      modifiedDate = modifiedDate,
      data = s"""
           |{
           |  "id": "i111",
           |  "updatedDate": "$modifiedDate",
           |  "comment": "Legacy line of lamentable leopards",
           |  "bibIds": ${JsonUtil.toJson(bibIds).get}
           |}""".stripMargin,
      bibIds = bibIds,
      unlinkedBibIds = unlinkedBibIds
    )
  }
}
