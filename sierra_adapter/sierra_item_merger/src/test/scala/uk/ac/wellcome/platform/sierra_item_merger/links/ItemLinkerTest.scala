package uk.ac.wellcome.platform.sierra_item_merger.links

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil

class ItemLinkerTest extends FunSpec with Matchers with SierraUtil {

  it("should add the item if it doesn't exist already") {
    val bibId = createSierraRecordNumber
    val itemRecord = createSierraItemRecordWith(
      bibIds = List(bibId)
    )

    val sierraTransformable = SierraTransformable(sierraId = bibId)
    val result = ItemLinker.linkItemRecord(sierraTransformable, itemRecord)

    result.itemData(itemRecord.id) shouldBe itemRecord
  }

  it("should update itemData when merging item records with newer data") {
    val bibId = createSierraRecordNumber
    val itemRecord = createSierraItemRecordWith(
      modifiedDate = olderDate,
      bibIds = List(bibId)
    )

    val sierraTransformable = SierraTransformable(
      sierraId = bibId,
      itemData = Map(itemRecord.id -> itemRecord)
    )

    val newerRecord = itemRecord.copy(
      modifiedDate = newerDate
    )
    val result = ItemLinker.linkItemRecord(sierraTransformable, newerRecord)

    result shouldBe sierraTransformable.copy(
      itemData = Map(newerRecord.id -> newerRecord))
  }

  it("should return itself when merging item records with stale data") {
    val bibId = createSierraRecordNumber
    val itemRecord = createSierraItemRecordWith(
      modifiedDate = newerDate,
      bibIds = List(bibId)
    )

    val itemId = "i111"
    val sierraTransformable = SierraTransformable(
      sierraId = bibId,
      itemData = Map(itemRecord.id -> itemRecord)
    )

    val oldRecord = itemRecord.copy(
      modifiedDate = olderDate
    )
    val result = ItemLinker.linkItemRecord(sierraTransformable, oldRecord)
    result shouldBe sierraTransformable
  }

  it("should support adding multiple items to a merged record") {
    val bibId = createSierraRecordNumber

    val record1 = createSierraItemRecordWith(bibIds = List(bibId))
    val record2 = createSierraItemRecordWith(bibIds = List(bibId))

    val sierraTransformable = SierraTransformable(sierraId = bibId)
    val result1 = ItemLinker.linkItemRecord(sierraTransformable, record1)
    val result2 = ItemLinker.linkItemRecord(result1, record2)

    result1.itemData(record1.id) shouldBe record1
    result2.itemData(record2.id) shouldBe record2
  }

  it("should only merge item records with matching bib IDs") {
    val bibId = createSierraRecordNumber
    val unrelatedBibId = createSierraRecordNumber

    val record = createSierraItemRecordWith(
      bibIds = List(bibId)
    )

    val sierraTransformable = SierraTransformable(sierraId = unrelatedBibId)

    val caught = intercept[RuntimeException] {
      ItemLinker.linkItemRecord(sierraTransformable, record)
    }

    caught.getMessage shouldEqual s"Non-matching bib id $bibId in item bib List($unrelatedBibId)"
  }
}
