package uk.ac.wellcome.platform.sierra_items_to_dynamo.sink

import io.circe.optics.JsonPath.root
import io.circe.parser._
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.SierraItemRecord
import uk.ac.wellcome.utils.JsonUtil

class SierraItemRecordMergerTest
    extends FunSpec
    with Matchers {

  it("should combine the bibIds in the final result") {
    val oldRecord = sierraItemRecord(bibIds = List("1", "2", "3"))
    val newRecord = sierraItemRecord(bibIds = List("1", "2", "3", "4", "5"))

    val mergedRecord = SierraItemRecordMerger.mergeItems(oldRecord = oldRecord, newRecord = newRecord)
    mergedRecord.bibIds shouldEqual List("1", "2", "3", "4", "5")
    mergedRecord.unlinkedBibIds shouldEqual List()
  }

  it("should record unlinked bibIds") {
    val oldRecord = sierraItemRecord(bibIds = List("1", "2", "3"))
    val newRecord = sierraItemRecord(bibIds = List("3", "4", "5"))

    val mergedRecord = SierraItemRecordMerger.mergeItems(oldRecord = oldRecord, newRecord = newRecord)
    mergedRecord.bibIds shouldEqual List("3", "4", "5")
    mergedRecord.unlinkedBibIds shouldEqual List("1", "2")
  }

  it("should preserve existing unlinked bibIds") {
    val oldRecord = sierraItemRecord(bibIds = List("1", "2", "3"), unlinkedBibIds = List("4", "5"))
    val newRecord = sierraItemRecord(bibIds = List("1", "2", "3"))

    val mergedRecord = SierraItemRecordMerger.mergeItems(oldRecord = oldRecord, newRecord = newRecord)
    mergedRecord.unlinkedBibIds shouldEqual List("4", "5")
  }

  it("should not duplicate unlinked bibIds") {
    // This would be an unusual scenario to arise, but check we handle it anyway!
    val oldRecord = sierraItemRecord(bibIds = List("1", "2", "3"), unlinkedBibIds = List("3"))
    val newRecord = sierraItemRecord(bibIds = List("1", "2"))

    val mergedRecord = SierraItemRecordMerger.mergeItems(oldRecord = oldRecord, newRecord = newRecord)
    mergedRecord.bibIds shouldEqual List("1", "2")
    mergedRecord.unlinkedBibIds shouldEqual List("3")
  }

  // previously unlinked ID becomes present

  private def sierraItemRecord(bibIds: List[String], unlinkedBibIds: List[String] = List()): SierraItemRecord = {
    SierraItemRecord(
      id = s"i111",
      modifiedDate = "2001-01-01T01:01:01Z",
      data =
        s"""
           |{
           |  "id": "i111",
           |  "updatedDate": "2001-01-01T01:01:01Z",
           |  "comment": "Legacy line of lamentable leopards",
           |  "bibIds": ${JsonUtil.toJson(bibIds).get}
           |}""".stripMargin,
      bibIds = bibIds,
      unlinkedBibIds = unlinkedBibIds
    )
  }
}
