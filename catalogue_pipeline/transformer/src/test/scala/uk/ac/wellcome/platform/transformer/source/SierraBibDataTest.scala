package uk.ac.wellcome.platform.transformer.source

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.models.work.test.util.IdentifiersUtil
import uk.ac.wellcome.utils.JsonUtil._

class SierraBibDataTest
    extends FunSpec
    with Matchers
    with IdentifiersUtil
    with SierraUtil {
  it("deserialises bib data from Sierra correctly") {
    val id = createSierraRecordNumberString
    val record = fromJson[SierraBibData](s"""
        |{
        |  "id": "$id",
        |  "title": "${randomAlphanumeric(25)}",
        |  "varFields": []
        |}
      """.stripMargin).get

    record.sierraId shouldBe SierraRecordNumber(id)
  }
}
