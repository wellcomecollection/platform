package uk.ac.wellcome.models.transformable

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraRecordNumber
}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraData

class SierraTransformableTest extends FunSpec with Matchers with SierraData {

  it("should allow creation of SierraTransformable with no data") {
    SierraTransformable(sierraId = SierraRecordNumber("1111111"))
  }

  it("should allow creation from only a SierraBibRecord") {
    val bibRecord = sierraBibRecord(id = "1010101")
    val mergedRecord = SierraTransformable(bibRecord = bibRecord)
    mergedRecord.sierraId shouldEqual bibRecord.id
    mergedRecord.maybeBibData.get shouldEqual bibRecord
  }

  def sierraBibRecord(
    id: String = "1111111",
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
