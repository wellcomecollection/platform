package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord
import uk.ac.wellcome.test.utils.SierraData

class SierraTransformableTest extends FunSpec with Matchers with SierraData {

  it("should allow creation of SierraTransformable with no data") {
    SierraTransformable(sourceId = "111")
  }

  it("should allow creation from only a SierraBibRecord") {
    val bibRecord = sierraBibRecord(id = "101")
    val mergedRecord = SierraTransformable(bibRecord = bibRecord)
    mergedRecord.sourceId shouldEqual bibRecord.id
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
    val record = SierraTransformable(sourceId = "555")
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
