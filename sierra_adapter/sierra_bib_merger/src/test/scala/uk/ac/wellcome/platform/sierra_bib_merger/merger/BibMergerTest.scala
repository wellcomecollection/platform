package uk.ac.wellcome.platform.sierra_bib_merger.merger

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraBibRecord

class BibMergerTest extends FunSpec with Matchers {

  describe("merging with a SierraBibRecord") {
    it("should merge data from a bibRecord when empty") {
      val record = sierraBibRecord(id = "222")
      val originalRecord = SierraTransformable(sourceId = "222")

      val newRecord = BibMerger.mergeBibRecord(originalRecord, record)
      newRecord.maybeBibData.get shouldEqual record
    }

    it("should only merge bib records with matching ids") {
      val record = sierraBibRecord(id = "333")
      val originalRecord = SierraTransformable(sourceId = "444")

      val caught = intercept[RuntimeException] {
        BibMerger.mergeBibRecord(originalRecord, record)
      }
      caught.getMessage shouldEqual "Non-matching bib ids 333 != 444"
    }

    it(
      "should return the unmerged record when merging bib records with stale data") {
      val record = sierraBibRecord(
        id = "777",
        title = "Olde McOlde Recorde",
        modifiedDate = "2001-01-01T01:01:01Z"
      )

      val sierraTransformable = SierraTransformable(
        sourceId = "777",
        maybeBibData = Some(
          sierraBibRecord(
            id = "777",
            title = "Shiny McNewRecordz 2.0",
            modifiedDate = "2009-09-09T09:09:09Z"
          )
        )
      )

      val result = BibMerger.mergeBibRecord(sierraTransformable, record)
      result shouldBe sierraTransformable
    }

    it("should update bibData when merging bib records with newer data") {
      val record = sierraBibRecord(
        id = "888",
        title = "New and Shiny Data",
        modifiedDate = "2011-11-11T11:11:11Z"
      )

      val sierraTransformable = SierraTransformable(
        sourceId = "888",
        maybeBibData = Some(
          sierraBibRecord(
            id = "888",
            title = "Old and Dusty Data",
            modifiedDate = "2001-01-01T01:01:01Z"
          )
        )
      )

      val result = BibMerger.mergeBibRecord(sierraTransformable, record)
      result.maybeBibData.get shouldBe record
    }
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
