package uk.ac.wellcome.models.transformable.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.utils.JsonUtil._

class SierraItemRecordTest extends FunSpec with Matchers with SierraUtil {

  it("can cast a SierraItemRecord to JSON and back again") {
    val originalRecord = createSierraItemRecord

    val jsonString = toJson(originalRecord).get
    val parsedRecord = fromJson[SierraItemRecord](jsonString).get
    parsedRecord shouldEqual originalRecord
  }
}
