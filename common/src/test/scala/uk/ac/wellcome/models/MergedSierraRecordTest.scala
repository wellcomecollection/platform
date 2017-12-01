package uk.ac.wellcome.models

import java.time.Instant

import org.scalatest.{FunSpec, Matchers}

class MergedSierraRecordTest extends FunSpec with Matchers {

  it("should allow creation of MergedSierraRecord with no data") {
    MergedSierraRecord(id = "100")
  }

  it("should merge data from a bibRecord when empty") {
    val sierraRecord = SierraRecord(id = "100", data = "", modifiedDate = Instant.now)
    val originalRecord = MergedSierraRecord(id = "100")

    val newRecord: Option[MergedSierraRecord] =
      originalRecord.mergeBibRecord(sierraRecord)

    newRecord.get.bibData.get shouldEqual sierraRecord.data
  }

  it("should only merge bib records with matching ids") {
    val sierraRecord = SierraRecord(id = "100", data = "", modifiedDate = Instant.now)
    val originalRecord = MergedSierraRecord(id = "200")

    val caught = intercept[RuntimeException] {
      originalRecord.mergeBibRecord(sierraRecord)
    }

    caught.getMessage shouldEqual "Non-matching bib ids 100 != 200"
  }

  it("should be at version 1 when first created") {
    val record = MergedSierraRecord(id = "300")
    record.version shouldEqual 1
  }

  it("should always increment the version when mergeBibRecord is called") {
    val sierraRecord = SierraRecord(id = "400", data = "", modifiedDate = Instant.now)
    val originalRecord = MergedSierraRecord(id = "400", version = 10)
    val newRecord = originalRecord.mergeBibRecord(sierraRecord)

    newRecord.get.version shouldEqual 11
  }

  it("should return None when merging bib records with stale data"){
    val sierraRecord = SierraRecord(
      id = "400",
      data = bibRecordString(
        id = "400",
        updatedDate = "2001-01-01T01:01:01Z",
        title = "Olde McOlde Recorde"
      ),
      modifiedDate = "2001-01-01T01:01:01Z"
    )

    val mergedSierraRecord = MergedSierraRecord(
      id = "400",
      bibData = Some(bibRecordString(
        id = "400",
        updatedDate = "2009-09-09T09:09:09Z",
        title = "Shiny McNewRecordz 2.0"
      ))
    )

    val result = mergedSierraRecord.mergeBibRecord(sierraRecord)

    result shouldBe None
  }


  def bibRecordString(id: String,
                      updatedDate: String,
                      title: String = "Lehrbuch und Atlas der Gastroskopie") =
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

