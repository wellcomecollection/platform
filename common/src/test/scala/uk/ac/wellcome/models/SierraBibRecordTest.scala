package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.circe.jsonUtil
import uk.ac.wellcome.circe.jsonUtil._
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord

class SierraBibRecordTest extends FunSpec with Matchers {

  it("can cast a SierraBibRecord to JSON and back again") {
    val originalRecord = SierraBibRecord(
      id = "b0123456",
      data = """{"title": "A jumping jaguar in a jungle of junipers"}""",
      modifiedDate = "2007-07-07T07:07:07Z"
    )

    val jsonString = jsonUtil.toJson(originalRecord).get
    val parsedRecord = jsonUtil.fromJson[SierraBibRecord](jsonString).get
    parsedRecord shouldEqual originalRecord
  }
}
