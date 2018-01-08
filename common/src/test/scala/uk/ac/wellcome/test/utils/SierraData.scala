package uk.ac.wellcome.test.utils

import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord

trait SierraData {

  def sierraItemRecord(
    id: String = "i111",
    title: String = "Ingenious imps invent invasive implements",
    modifiedDate: String = "2001-01-01T01:01:01Z",
    bibIds: List[String] = List(),
    unlinkedBibIds: List[String] = List()
  ) = SierraItemRecord(
    id = id,
    data = sierraRecordString(
      id = id,
      updatedDate = modifiedDate,
      title = title
    ),
    modifiedDate = modifiedDate,
    bibIds = bibIds,
    unlinkedBibIds = unlinkedBibIds
  )

  def sierraRecordString(
    id: String,
    updatedDate: String,
    title: String
  ) =
    s"""
       |{
       |      "id": "$id",
       |      "updatedDate": "$updatedDate",
       |      "createdDate": "1999-11-01T16:36:51Z",
       |      "deleted": false,
       |      "suppressed": false,
       |      "lang": {
       |        "code": "ger",
       |        "name": "German"
       |      },
       |      "title": "$title",
       |      "author": "Schindler, Rudolf, 1888-",
       |      "materialType": {
       |        "code": "a",
       |        "value": "Books"
       |      },
       |      "bibLevel": {
       |        "code": "m",
       |        "value": "MONOGRAPH"
       |      },
       |      "publishYear": 1923,
       |      "catalogDate": "1999-01-01",
       |      "country": {
       |        "code": "gw ",
       |        "name": "Germany"
       |      }
       |    }
    """.stripMargin

}
