package uk.ac.wellcome.models

import java.time.Instant

import org.scalatest.{FunSpec, Matchers}

import uk.ac.wellcome.utils.JsonUtil

class MergedSierraRecordTest extends FunSpec with Matchers {

  it("should allow creation of MergedSierraRecord with no data") {
    MergedSierraRecord(id = "111")
  }

  it("should allow creation from only a SierraBibRecord") {
    val bibRecord = sierraBibRecord(id = "101")
    val mergedRecord = MergedSierraRecord(bibRecord = bibRecord)
    mergedRecord.id shouldEqual bibRecord.id
    mergedRecord.bibData.get shouldEqual bibRecord
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

  it("should merge data from a bibRecord when empty") {
    val record = sierraBibRecord(id = "222")
    val originalRecord = MergedSierraRecord(id = "222")

    val newRecord: Option[MergedSierraRecord] =
      originalRecord.mergeBibRecord(record)

    newRecord.get.bibData.get shouldEqual record
  }

  it("should only merge bib records with matching ids") {
    val record = sierraBibRecord(id = "333")
    val originalRecord = MergedSierraRecord(id = "444")

    val caught = intercept[RuntimeException] {
      originalRecord.mergeBibRecord(record)
    }
    caught.getMessage shouldEqual "Non-matching bib ids 333 != 444"
  }

  it("should be at version 1 when first created") {
    val record = MergedSierraRecord(id = "555")
    record.version shouldEqual 1
  }

  it("should always increment the version when mergeBibRecord is called") {
    val record = sierraBibRecord(id = "666")
    val originalRecord = MergedSierraRecord(id = "666", version = 10)
    val newRecord = originalRecord.mergeBibRecord(record)
    newRecord.get.version shouldEqual 11
  }

  it("should return None when merging bib records with stale data"){
    val record = sierraBibRecord(
      id = "777",
      title = "Olde McOlde Recorde",
      modifiedDate = "2001-01-01T01:01:01Z"
    )

    val mergedSierraRecord = MergedSierraRecord(
      id = "777",
      bibData = Some(
        sierraBibRecord(
          id = "777",
          title = "Shiny McNewRecordz 2.0",
          modifiedDate = "2009-09-09T09:09:09Z"
        )
      )
    )

    val result = mergedSierraRecord.mergeBibRecord(record)
    result shouldBe None
  }

  it("should update bibData when merging bib records with newer data") {
    val record = sierraBibRecord(
      id = "888",
      title = "New and Shiny Data",
      modifiedDate = "2011-11-11T11:11:11Z"
    )

    val mergedSierraRecord = MergedSierraRecord(
      id = "888",
      bibData = Some(
        sierraBibRecord(
          id = "888",
          title = "Old and Dusty Data",
          modifiedDate = "2001-01-01T01:01:01Z"
        )
      )
    )

    val result = mergedSierraRecord.mergeBibRecord(record)
    result.get.bibData.get shouldBe record
  }

  def sierraBibRecord(
    id: String = "111",
    title: String = "Two toucans touching a towel",
    modifiedDate: String = "2001-01-01T01:01:01Z"
  ) = SierraBibRecord(
    id = id,
    data = bibRecordString(id = id, updatedDate = modifiedDate, title = title),
    modifiedDate = modifiedDate
  )

  it("should support creation directly from a SierraBibRecord") {
    val record = sierraBibRecord(id = "999")
    val mergedRecord = MergedSierraRecord(bibRecord = record)
    mergedRecord.id shouldEqual record.id
  }

  def sierraBibRecordString(id: String, modifiedDate: String, title: String) =
    JsonUtil.toJson(SierraBibRecord(
      id = id,
      data = bibRecordString(
        id = id,
        updatedDate = modifiedDate,
        title = title
      ),
      modifiedDate = modifiedDate
    )).get

  private def bibRecordString(
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

