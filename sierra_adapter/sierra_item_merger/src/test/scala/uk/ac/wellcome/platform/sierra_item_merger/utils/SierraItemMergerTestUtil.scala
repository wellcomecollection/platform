package uk.ac.wellcome.platform.sierra_item_merger.utils

import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.utils.JsonUtil._

trait SierraItemMergerTestUtil {

  private def itemRecordString(id: String,
                               updatedDate: String,
                               bibIds: List[String] = List()) =
    s"""
       |{
       |  "id": "$id",
       |  "updatedDate": "$updatedDate",
       |  "deleted": false,
       |  "bibIds": ${toJson(bibIds).get},
       |  "fixedFields": {
       |    "85": {
       |      "label": "REVISIONS",
       |      "value": "1"
       |    },
       |    "86": {
       |      "label": "AGENCY",
       |      "value": "1"
       |    }
       |  },
       |  "varFields": [
       |    {
       |      "fieldTag": "c",
       |      "marcTag": "949",
       |      "ind1": " ",
       |      "ind2": " ",
       |      "subfields": [
       |        {
       |          "tag": "a",
       |          "content": "5722F"
       |        }
       |      ]
       |    }
       |  ]
       |}""".stripMargin

  protected def sierraItemRecord(
    id: String,
    updatedDate: String,
    bibIds: List[String]
  ): SierraItemRecord =
    SierraItemRecord(
      id = id,
      data = itemRecordString(
        id = id,
        updatedDate = updatedDate,
        bibIds = bibIds
      ),
      bibIds = bibIds,
      modifiedDate = updatedDate
    )
}
