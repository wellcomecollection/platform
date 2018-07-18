package uk.ac.wellcome.models.transformable.sierra

import java.time.Instant

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.utils.JsonUtil

class SierraBibRecordTest extends FunSpec with Matchers {

  it("can cast a SierraBibRecord to JSON and back again") {
    val originalRecord = SierraBibRecord(
      id = "1234567",
      data = """{"title": "A jumping jaguar in a jungle of junipers"}""",
      modifiedDate = "2007-07-07T07:07:07Z"
    )

    val jsonString = JsonUtil.toJson(originalRecord).get
    val parsedRecord = JsonUtil.fromJson[SierraBibRecord](jsonString).get
    parsedRecord shouldEqual originalRecord
  }

  it("does not allow creating a SierraBibRecord with an ID that isn't 7 digits") {
    val caught = intercept[IllegalArgumentException] {
      SierraBibRecord(
        id = "b0123456",
        data = """{"title": "A jumping jaguar in a jungle of junipers"}""",
        modifiedDate = Instant.now()
      )
    }

    caught.getMessage shouldBe "requirement failed: Not a 7-digit Sierra record number: b0123456"
  }
}
