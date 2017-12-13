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

  it("should merge data from a bibRecord when empty") {
    val record = sierraBibRecord(id = "222")
    val originalRecord = MergedSierraRecord(id = "222")

    val newRecord = originalRecord.mergeBibRecord(record)
    newRecord.maybeBibData.get shouldEqual record
  }

  it("should only merge bib records with matching ids") {
    val record = sierraBibRecord(id = "333")
    val originalRecord = MergedSierraRecord(id = "444")

    val caught = intercept[RuntimeException] {
      originalRecord.mergeBibRecord(record)
    }
    caught.getMessage shouldEqual "Non-matching bib ids 333 != 444"
  }

  it("should be at version 0 when first created") {
    val record = MergedSierraRecord(id = "555")
    record.version shouldEqual 0
  }

  it("should never increment the version when mergeBibRecord is called") {
    val record = sierraBibRecord(id = "666")
    val originalRecord = MergedSierraRecord(id = "666", version = 10)
    val newRecord = originalRecord.mergeBibRecord(record)
    newRecord.version shouldEqual 10
  }

  it("should return None when merging bib records with stale data") {
    val record = sierraBibRecord(
      id = "777",
      title = "Olde McOlde Recorde",
      modifiedDate = "2001-01-01T01:01:01Z"
    )

    val mergedSierraRecord = MergedSierraRecord(
      id = "777",
      maybeBibData = Some(
        sierraBibRecord(
          id = "777",
          title = "Shiny McNewRecordz 2.0",
          modifiedDate = "2009-09-09T09:09:09Z"
        )
      )
    )

    val result = mergedSierraRecord.mergeBibRecord(record)
    result shouldBe mergedSierraRecord
  }

  it("should update bibData when merging bib records with newer data") {
    val record = sierraBibRecord(
      id = "888",
      title = "New and Shiny Data",
      modifiedDate = "2011-11-11T11:11:11Z"
    )

    val mergedSierraRecord = MergedSierraRecord(
      id = "888",
      maybeBibData = Some(
        sierraBibRecord(
          id = "888",
          title = "Old and Dusty Data",
          modifiedDate = "2001-01-01T01:01:01Z"
        )
      )
    )

    val result = mergedSierraRecord.mergeBibRecord(record)
    result.maybeBibData.get shouldBe record
  }

  it("should add itemData when there isn't already an item") {
    val record = sierraItemRecord(
      id = "i888",
      title = "Illustrious imps are ingenious",
      modifiedDate = "2008-08-08T08:08:08Z"
    )

    val mergedSierraRecord = MergedSierraRecord(id = "b888")
    val result = mergedSierraRecord.mergeItemRecord(record).get

    result.itemData.get(record.id).get shouldBe record
  }

  it("should overwrite existing itemData when there's a newer update") {
    val record = sierraItemRecord(
      id = "i999",
      title = "No, new narwhals are never naughty",
      modifiedDate = "2009-09-09T09:09:09Z"
    )
    val mergedSierraRecord = MergedSierraRecord(
      id = "b999",
      itemData = Map(record.id -> record)
    )

    val newerRecord = sierraItemRecord(
      id = record.id,
      title = "Nobody noticed the naughty narwhals",
      modifiedDate = "2010-10-10T10:10:10Z"
    )
    val result = mergedSierraRecord.mergeItemRecord(newerRecord).get

    result.itemData.get(record.id).get shouldBe newerRecord
  }

  it("should not overwrite existing itemData when there's a older update") {
    val record = sierraItemRecord(
      id = "i111",
      title = "Only otters occupy the orange oval",
      modifiedDate = "2001-01-01T01:01:01Z"
    )
    val mergedSierraRecord = MergedSierraRecord(
      id = "b111",
      itemData = Map(record.id -> record)
    )

    val newerRecord = sierraItemRecord(
      id = record.id,
      title = "Old otters outside the oblong",
      modifiedDate = "2000-01-01T01:01:01Z"
    )
    val result = mergedSierraRecord.mergeItemRecord(newerRecord)
    result shouldBe None
  }

  it("should support adding multiple items to a merged record") {
    val record1 = sierraItemRecord(
      id = "i111",
      title = "Outside the orangutan opens an orange",
      modifiedDate = "2001-01-01T01:01:01Z"
    )
    val record2 = sierraItemRecord(
      id = "i222",
      title = "Twice the turtles took the turn",
      modifiedDate = "2002-02-02T02:02:02Z"
    )

    val mergedSierraRecord = MergedSierraRecord(id = "b121")
    val result1 = mergedSierraRecord.mergeItemRecord(record1).get
    val result2 = result1.mergeItemRecord(record2).get

    result1.itemData.get(record1.id).get shouldBe record1
    result2.itemData.get(record2.id).get shouldBe record2
  }

  it("should not perform a transformation without bibData") {
    val mergedSierraRecord =
      MergedSierraRecord(id = "000", maybeBibData = None)

    val transformedSierraRecord = mergedSierraRecord.transform
    transformedSierraRecord.isSuccess shouldBe true

    transformedSierraRecord.get shouldBe None
  }

  it("should not perform a transformation, even if some itemData is present") {
    val mergedSierraRecord = MergedSierraRecord(
      id = "b111",
      maybeBibData = None,
      itemData = Map(
        "i111" -> sierraItemRecord(
          id = "i111",
          title = "An incomplete invocation of items",
          modifiedDate = "2001-01-01T01:01:01Z"
        ))
    )

    val transformedSierraRecord = mergedSierraRecord.transform
    transformedSierraRecord.isSuccess shouldBe true
    transformedSierraRecord.get shouldBe None
  }

  it("should transform itself into a work") {
    val id = "000"
    val title = "Hi Diddle Dee Dee"
    val data =
      s"""
        |{
        | "id": "$id",
        | "title": "$title"
        |}
      """.stripMargin

    val mergedSierraRecord = MergedSierraRecord(
      id = id,
      maybeBibData = Some(
        SierraBibRecord(id = id, data = data, modifiedDate = Instant.now())))

    val transformedSierraRecord = mergedSierraRecord.transform
    transformedSierraRecord.isSuccess shouldBe true

    val identifier = SourceIdentifier(IdentifierSchemes.sierraSystemNumber, id)

    transformedSierraRecord.get shouldBe Some(
      Work(
        title = title,
        sourceIdentifier = identifier,
        identifiers = List(identifier)
      )
    )
  }

  it("should support creation directly from a SierraBibRecord") {
    val record = sierraBibRecord(id = "999")
    val mergedRecord = MergedSierraRecord(bibRecord = record)
    mergedRecord.id shouldEqual record.id
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
    modifiedDate: String = "2001-01-01T01:01:01Z"
  ) = SierraItemRecord(
    id = id,
    data = sierraRecordString(
      id = id,
      updatedDate = modifiedDate,
      title = title
    ),
    modifiedDate = modifiedDate,
    bibIds = List("b1111")
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
