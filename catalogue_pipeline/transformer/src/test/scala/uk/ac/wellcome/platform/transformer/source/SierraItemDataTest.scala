package uk.ac.wellcome.platform.transformer.source

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.utils.JsonUtil._

class SierraItemDataTest extends FunSpec with Matchers with SierraUtil {
  it("can serialise from JSON") {
    val id = createSierraRecordNumber

    // The structure of this example is based on the item data for
    // Sierra record 1000141, as retrieved on 2018-07-24.
    val jsonString =
      s"""
        |{
        |  "deleted": false,
        |  "fixedFields": {
        |    "100": {
        |      "label": "OPACMSG",
        |      "value": "f"
        |    }
        |  },
        |  "id": "$id",
        |  "location": {
        |    "code": "sgmed",
        |    "name": "Closed stores Med."
        |  },
        |  "varFields": [
        |    {
        |      "fieldTag": "a",
        |      "ind1": "0",
        |      "ind2": "0",
        |      "marcTag": "949",
        |      "subfields": [
        |        {
        |          "content": "STAX",
        |          "tag": "1"
        |        },
        |        {
        |          "content": "sgmed",
        |          "tag": "2"
        |        }
        |      ]
        |    },
        |    {
        |      "content": "22101636084",
        |      "fieldTag": "b"
        |    }
        |  ]
        |}
      """.stripMargin

    val itemData = fromJson[SierraItemData](jsonString).get
    itemData.id shouldBe id
  }
}
