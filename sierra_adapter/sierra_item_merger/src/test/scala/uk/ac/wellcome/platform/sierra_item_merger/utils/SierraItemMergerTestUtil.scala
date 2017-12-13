package uk.ac.wellcome.platform.sierra_item_merger.utils

import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.query.UniqueKey
import org.scalatest.{Assertion, Matchers, Suite}
import org.scalatest.concurrent.Eventually
import uk.ac.wellcome.models.SierraItemRecord
import uk.ac.wellcome.platform.sierra_item_merger.locals.DynamoDBLocal
import uk.ac.wellcome.utils.JsonUtil
import com.gu.scanamo.syntax._

trait SierraItemMergerTestUtil extends DynamoDBLocal with Matchers {
  this: Suite =>

  private def itemRecordString(id: String,
                               updatedDate: String,
                               bibIds: List[String] = List()) =
    s"""
       |{
       |  "id": "$id",
       |  "updatedDate": "$updatedDate",
       |  "deleted": false,
       |  "bibIds": ${JsonUtil.toJson(bibIds).get},
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

  protected def dynamoQueryEqualsValue[T: DynamoFormat](id: String)(
    expectedValue: T): Assertion = {

    val actualValue = Scanamo.get[T](dynamoDbClient)(tableName)('id -> id).get
    actualValue shouldBe Right(expectedValue)
  }
}
