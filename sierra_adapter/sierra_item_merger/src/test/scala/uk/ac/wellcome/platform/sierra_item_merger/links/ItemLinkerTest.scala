package uk.ac.wellcome.platform.sierra_item_merger.links

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord

class ItemLinkerTest extends FunSpec with Matchers {

  it("should add the item if it doesn't exist already") {
    val record = sierraItemRecord(
      id = "i888",
      title = "Illustrious imps are ingenious",
      modifiedDate = "2008-08-08T08:08:08Z",
      bibIds = List("b888")
    )

    val sierraTransformable = SierraTransformable(id = "b888")
    val result = ItemLinker.linkItemRecord(sierraTransformable, record)

    result.itemData(record.id) shouldBe record
  }

  it("should update itemData when merging item records with newer data") {
    val itemId = "i999"
    val sierraTransformable = SierraTransformable(
      id = "b999",
      itemData = Map(
        itemId -> sierraItemRecord(
          id = itemId,
          title = "No, new narwhals are never naughty",
          modifiedDate = "2009-09-09T09:09:09Z",
          bibIds = List("b999")
        ))
    )

    val newerRecord = sierraItemRecord(
      id = itemId,
      title = "Nobody noticed the naughty narwhals",
      modifiedDate = "2010-10-10T10:10:10Z",
      bibIds = List("b999")
    )
    val result = ItemLinker.linkItemRecord(sierraTransformable, newerRecord)

    result shouldBe sierraTransformable.copy(
      itemData = Map(itemId -> newerRecord))
  }

  it("should return itself when merging item records with stale data") {
    val itemId = "i111"
    val sierraTransformable = SierraTransformable(
      id = "b111",
      itemData = Map(
        itemId -> sierraItemRecord(
          id = itemId,
          title = "Only otters occupy the orange oval",
          modifiedDate = "2001-01-01T01:01:01Z",
          bibIds = List("b111")
        ))
    )

    val oldRecord = sierraItemRecord(
      id = itemId,
      title = "Old otters outside the oblong",
      modifiedDate = "2000-01-01T01:01:01Z",
      bibIds = List("b111")
    )
    val result = ItemLinker.linkItemRecord(sierraTransformable, oldRecord)
    result shouldBe sierraTransformable
  }

  it("should support adding multiple items to a merged record") {
    val record1 = sierraItemRecord(
      id = "i111",
      title = "Outside the orangutan opens an orange",
      modifiedDate = "2001-01-01T01:01:01Z",
      bibIds = List("b121")
    )
    val record2 = sierraItemRecord(
      id = "i222",
      title = "Twice the turtles took the turn",
      modifiedDate = "2002-02-02T02:02:02Z",
      bibIds = List("b121")
    )

    val sierraTransformable = SierraTransformable(id = "b121")
    val result1 = ItemLinker.linkItemRecord(sierraTransformable, record1)
    val result2 = ItemLinker.linkItemRecord(result1, record2)

    result1.itemData(record1.id) shouldBe record1
    result2.itemData(record2.id) shouldBe record2
  }

  it("should only merge item records with matching bib IDs") {
    val bibId = "444"
    val unrelatedBibId = "666"

    val record = sierraItemRecord(
      id = "i999",
      title = "Only otters occupy the orange oval",
      modifiedDate = "2001-01-01T01:01:01Z",
      bibIds = List(unrelatedBibId),
      unlinkedBibIds = List()
    )

    val sierraTransformable = SierraTransformable(id = bibId)

    val caught = intercept[RuntimeException] {
      ItemLinker.linkItemRecord(sierraTransformable, record)
    }

    caught.getMessage shouldEqual "Non-matching bib id 444 in item bib List(666)"
  }

  def sierraItemRecord(
    id: String = "i111",
    title: String = "Ingenious imps invent invasive implements",
    modifiedDate: String = "2001-01-01T01:01:01Z",
    bibIds: List[String],
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
