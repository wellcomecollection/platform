package uk.ac.wellcome.models.transformable

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil

class SierraTransformableTest extends FunSpec with Matchers with SierraUtil {

  it("allows creation of SierraTransformable with no data") {
    SierraTransformable(sierraId = createSierraRecordNumber)
  }

  it("allows creation from only a SierraBibRecord") {
    val bibRecord = createSierraBibRecord
    val mergedRecord = SierraTransformable(bibRecord = bibRecord)
    mergedRecord.sierraId shouldEqual bibRecord.id
    mergedRecord.maybeBibRecord.get shouldEqual bibRecord
  }

  it("has the correct ID") {
    val sierraId = createSierraRecordNumber
    SierraTransformable(sierraId = sierraId).id shouldBe s"sierra/${sierraId.withoutCheckDigit}"
  }
}
