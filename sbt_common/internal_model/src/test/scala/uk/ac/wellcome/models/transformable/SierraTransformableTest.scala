package uk.ac.wellcome.models.transformable

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.models.transformable.SierraTransformableCodec._

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

  it("can serialise a SierraTransformable via JSON") {
    val itemRecords = (1 to 3)
      .map { _ => createSierraItemRecord }
      .map { itemRecord => itemRecord.id -> itemRecord }
      .toMap

    val transformable = SierraTransformable(
      sierraId = createSierraRecordNumber,
      maybeBibRecord = Some(createSierraBibRecord),
      itemRecords = itemRecords
    )

    val jsonString = toJson(transformable).get
    val parsedJson = fromJson[SierraTransformable](jsonString).get

    parsedJson shouldBe transformable
  }
}
