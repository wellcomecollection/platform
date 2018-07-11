package uk.ac.wellcome.models.transformable.sierra.test.utils

import java.time.Instant

import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord

import scala.util.Random

trait SierraUtil {

  def createSierraBibRecordStringWith(id: String): String =
    s"""
       |{
       |      "id": "$id",
       |      "updatedDate": "2001-01-01T01:01:01Z",
       |      "createdDate": "1999-09-09T09:09:09Z",
       |      "deleted": false,
       |      "suppressed": false,
       |      "lang": {
       |        "code": "ger",
       |        "name": "German"
       |      },
       |      "title": "This is the title of the work",
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

  def createSierraBibRecordWith(
    id: String = Random.alphanumeric take 7 mkString,
    data: String = "",
    modifiedDate: Instant = Instant.now
  ): SierraBibRecord =
    SierraBibRecord(
      id = id,
      data = if (data == "") createSierraBibRecordStringWith(id = id) else data,
      modifiedDate = modifiedDate
    )

  def createSierraBibRecord: SierraBibRecord = createSierraBibRecordWith()
}
