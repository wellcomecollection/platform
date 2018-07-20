package uk.ac.wellcome.models.transformable

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.utils.JsonUtil._

class SierraTransformableTest extends FunSpec with Matchers with SierraUtil {

  it("allows creating a SierraTransformable with no data") {
    SierraTransformable(sierraId = createSierraRecordNumber)
  }

  it("allows creating from only a SierraBibRecord") {
    val bibRecord = createSierraBibRecord
    val mergedRecord = SierraTransformable(bibRecord = bibRecord)
    mergedRecord.sierraId shouldEqual bibRecord.id
    mergedRecord.maybeBibRecord.get shouldEqual bibRecord
  }

  it("can decode/encode a SierraTransformable via JSON") {
    val transformable = SierraTransformable(sierraId = createSierraRecordNumber)

    val jsonString = toJson(transformable).get
    val parsedJson = fromJson[SierraTransformable](jsonString).get

    parsedJson shouldBe transformable
  }
}
