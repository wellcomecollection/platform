package uk.ac.wellcome.models.transformable

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil

class SierraTransformableTest extends FunSpec with Matchers with SierraUtil {

  it("should allow creation of SierraTransformable with no data") {
    SierraTransformable(sierraId = SierraRecordNumber("1111111"))
  }

  it("should allow creation from only a SierraBibRecord") {
    val bibRecord = createSierraBibRecord
    val mergedRecord = SierraTransformable(bibRecord = bibRecord)
    mergedRecord.sierraId shouldEqual bibRecord.id
    mergedRecord.maybeBibData.get shouldEqual bibRecord
  }
}
