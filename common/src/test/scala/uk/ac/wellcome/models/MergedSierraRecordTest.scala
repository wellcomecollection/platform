package uk.ac.wellcome.models

import java.time.Instant
import java.time.Instant._

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.MergedSierraRecord
import uk.ac.wellcome.utils.JsonUtil

class MergedSierraRecordTest extends FunSpec with Matchers {

  it("should allow creation of MergedSierraRecord with no data") {
    MergedSierraRecord(id = "111")
  }

  it("should allow creation from only a SierraBibRecord") {
    val bibRecord = sierraBibRecord(id = "101")
    val mergedRecord = MergedSierraRecord(bibRecord = bibRecord)
    mergedRecord.id shouldEqual bibRecord.id
    mergedRecord.maybeBibData.get shouldEqual bibRecord
  }

  it("should allow creation from a SierraBibRecord and a version") {
    val bibRecord = sierraBibRecord(id = "202")
    val version = 10
    val mergedRecord = MergedSierraRecord(
      bibRecord = bibRecord,
      version = version
    )
    mergedRecord.version shouldEqual version
  }

  it("should be at version 0 when first created") {
    val record = MergedSierraRecord(id = "555")
    record.version shouldEqual 0
  }

  def sierraBibRecord(
    id: String = "111",
    title: String = "Two toucans touching a towel",
    modifiedDate: String = "2001-01-01T01:01:01Z"
  ) = SierraBibRecord(
    id = id,
    data = sierraRecordString(
      id = id,
      updatedDate = modifiedDate,
      title = title
    ),
    modifiedDate = modifiedDate
  )

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

  private def sierraRecordString(
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
