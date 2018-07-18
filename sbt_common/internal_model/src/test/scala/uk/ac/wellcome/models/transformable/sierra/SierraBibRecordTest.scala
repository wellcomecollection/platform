package uk.ac.wellcome.models.transformable.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.utils.JsonUtil._

class SierraBibRecordTest extends FunSpec with Matchers with SierraUtil {

  it("can cast a SierraBibRecord to JSON and back again") {
    val originalRecord = SierraBibRecord(
      id = "1234567",
      data = """{"title": "A jumping jaguar in a jungle of junipers"}""",
      modifiedDate = "2007-07-07T07:07:07Z"
    )

    val jsonString = toJson(originalRecord).get
    val parsedRecord = fromJson[SierraBibRecord](jsonString).get
    parsedRecord shouldEqual originalRecord
  }

  it("does not allow creating a SierraBibRecord with an ID that isn't 7 digits") {
    val caught = intercept[IllegalArgumentException] {
      createSierraBibRecordWith(
        id = "b0123456"
      )
    }

    caught.getMessage shouldBe "requirement failed: Not a 7-digit Sierra record number: b0123456"
  }
}
