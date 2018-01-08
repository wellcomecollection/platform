package uk.ac.wellcome.models

import java.time.Instant
import java.time.Instant._

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.{SierraBibRecord, SierraItemRecord}
import uk.ac.wellcome.test.utils.SierraData
import uk.ac.wellcome.utils.JsonUtil

class SierraTransformableTest extends FunSpec with Matchers with SierraData {

  it("should allow creation of SierraTransformable with no data") {
    SierraTransformable(id = "111")
  }

  it("should allow creation from only a SierraBibRecord") {
    val bibRecord = sierraBibRecord(id = "101")
    val mergedRecord = SierraTransformable(bibRecord = bibRecord)
    mergedRecord.id shouldEqual bibRecord.id
    mergedRecord.maybeBibData.get shouldEqual bibRecord
  }

  it("should allow creation from a SierraBibRecord and a version") {
    val bibRecord = sierraBibRecord(id = "202")
    val version = 10
    val mergedRecord = SierraTransformable(
      bibRecord = bibRecord,
      version = version
    )
    mergedRecord.version shouldEqual version
  }

  it("should be at version 0 when first created") {
    val record = SierraTransformable(id = "555")
    record.version shouldEqual 0
  }

  def sierraBibRecord(
    id: String = "111",
    title: String = "Two toucans touching a towel",
    modifiedDate: String = "2001-01-01T01:01:01Z"
  ) = SierraBibRecord(
    id = id,
    data = sierraRecordString(
      id = id,
      updatedDate = modifiedDate,
      title = title
    ),
    modifiedDate = modifiedDate
  )

}
