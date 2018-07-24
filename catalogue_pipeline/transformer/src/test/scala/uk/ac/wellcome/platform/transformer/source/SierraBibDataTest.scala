package uk.ac.wellcome.platform.transformer.source

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.platform.transformer.source.SierraBibData._
import uk.ac.wellcome.utils.JsonUtil._

class SierraBibDataTest extends FunSpec with Matchers with SierraUtil {
  it("can serialise from JSON") {
    val id = createSierraRecordNumber

    // The structure of this example is based in part on the bib data
    // for Sierra record 1000120, as retrieved on 2018-07-24.
    val jsonString =
      s"""
         |{
         |  "available": true,
         |  "country": {
         |    "code": "${randomAlphanumeric(3)}",
         |    "name": "${randomAlphanumeric(10)}"
         |  },
         |  "deleted": false,
         |  "fixedFields": {
         |    "107": {
         |      "label": "MARCTYPE",
         |      "value": " "
         |    }
         |  },
         |  "id": "$id",
         |  "lang": {
         |    "code": "${randomAlphanumeric(3)}",
         |    "name": "${randomAlphanumeric(10)}"
         |  },
         |  "locations": [
         |    {
         |      "code": "${randomAlphanumeric(4)}",
         |      "name": "${randomAlphanumeric(25)}"
         |    }
         |  ],
         |  "varFields": []
         |}
       """.stripMargin

    val bibData = fromJson[SierraBibData](jsonString).get
    bibData.id shouldBe id
  }
}
