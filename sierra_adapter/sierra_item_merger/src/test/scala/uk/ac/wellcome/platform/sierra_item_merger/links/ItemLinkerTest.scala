package uk.ac.wellcome.platform.sierra_item_merger.links

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.test.utils.{SierraData, SierraUtil}

class ItemLinkerTest extends FunSpec with Matchers with SierraData with SierraUtil {

  it("should add the item if it doesn't exist already") {
    val record = createSierraItemRecordWith(
      bibIds = List("b888")
    )

    val sierraTransformable = SierraTransformable(sourceId = "b888")
    val result = ItemLinker.linkItemRecord(sierraTransformable, record)

    result.itemData(record.id) shouldBe record
  }

  it("should update itemData when merging item records with newer data") {
    val olderRecord = createSierraItemRecordWith(
      data = "<<older data>>",
      modifiedDate = "1999-09-09T09:09:09Z",
      bibIds = List("b999")
    )

    val sierraTransformable = SierraTransformable(
      sourceId = "b999",
      itemData = Map(olderRecord.id -> olderRecord)
    )

    val newerRecord = createSierraItemRecordWith(
      id = olderRecord.id,
      data = "<<newer data>>",
      modifiedDate = "2001-01-01T01:01:01Z",
      bibIds = olderRecord.bibIds
    )
    val result = ItemLinker.linkItemRecord(sierraTransformable, newerRecord)

    result shouldBe sierraTransformable.copy(
      itemData = Map(olderRecord.id -> newerRecord))
  }

  it("should return itself when merging item records with stale data") {
    val itemId = "i111"

    val newerRecord = createSierraItemRecordWith(
      data = "<<newer data>>",
      modifiedDate = "2001-01-01T01:01:01Z",
      bibIds = List("b111")
    )

    val sierraTransformable = SierraTransformable(
      sourceId = "b111",
      itemData = Map(newerRecord.id -> newerRecord)
    )

    val oldRecord = createSierraItemRecordWith(
      id = newerRecord.id,
      data = "<<older data>>",
      modifiedDate = "1999-09-09T09:09:09Z",
      bibIds = newerRecord.bibIds
    )

    val result = ItemLinker.linkItemRecord(sierraTransformable, oldRecord)
    result shouldBe sierraTransformable
  }

  it("should support adding multiple items to a merged record") {
    val record1 = createSierraItemRecordWith(
      bibIds = List("b121")
    )
    val record2 = createSierraItemRecordWith(
      bibIds = List("b121")
    )

    val sierraTransformable = SierraTransformable(sourceId = "b121")
    val result1 = ItemLinker.linkItemRecord(sierraTransformable, record1)
    val result2 = ItemLinker.linkItemRecord(result1, record2)

    result1.itemData(record1.id) shouldBe record1
    result2.itemData(record2.id) shouldBe record2
  }

  it("should only merge item records with matching bib IDs") {
    val bibId = "444"
    val unrelatedBibId = "666"

    val record = createSierraItemRecordWith(
      bibIds = List(unrelatedBibId)
    )

    val sierraTransformable = SierraTransformable(sourceId = bibId)

    val caught = intercept[RuntimeException] {
      ItemLinker.linkItemRecord(sierraTransformable, record)
    }

    caught.getMessage shouldEqual s"Non-matching bib id $bibId in item bib List($unrelatedBibId)"
  }
}
