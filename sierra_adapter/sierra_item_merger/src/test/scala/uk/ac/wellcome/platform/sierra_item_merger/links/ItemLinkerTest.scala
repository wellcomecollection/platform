package uk.ac.wellcome.platform.sierra_item_merger.links

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil

class ItemLinkerTest extends FunSpec with Matchers with SierraUtil {

  it("adds the item if it doesn't exist already") {
    val bibId = createSierraRecordNumberString
    val record = createSierraItemRecordWith(
      bibIds = List(bibId)
    )

    val sierraTransformable = SierraTransformable(sourceId = bibId)
    val result = ItemLinker.linkItemRecord(sierraTransformable, record)

    result.itemData shouldBe Map(record.id -> record)
  }

  it("updates itemData when merging item records with newer data") {
    val bibId = createSierraRecordNumberString
    val itemRecord = createSierraItemRecordWith(
      modifiedDate = olderDate,
      bibIds = List(bibId)
    )

    val sierraTransformable = SierraTransformable(
      sourceId = bibId,
      itemData = Map(itemRecord.id -> itemRecord)
    )

    val newerRecord = itemRecord.copy(
      data = """{"hey": "some new data"}""",
      modifiedDate = newerDate,
      bibIds = List(bibId)
    )
    val result = ItemLinker.linkItemRecord(sierraTransformable, newerRecord)

    result shouldBe sierraTransformable.copy(
      itemData = Map(itemRecord.id -> newerRecord))
  }

  it("returns itself when merging item records with stale data") {
    val bibId = createSierraRecordNumberString
    val itemRecord = createSierraItemRecordWith(
      modifiedDate = newerDate,
      bibIds = List(bibId)
    )

    val sierraTransformable = SierraTransformable(
      sourceId = bibId,
      itemData = Map(itemRecord.id -> itemRecord)
    )

    val oldRecord = itemRecord.copy(
      modifiedDate = olderDate,
      data = """{"older": "data goes here"}"""
    )
    val result = ItemLinker.linkItemRecord(sierraTransformable, oldRecord)
    result shouldBe sierraTransformable
  }

  it("supports adding multiple items to a merged record") {
    val bibId = createSierraRecordNumberString
    val record1 = createSierraItemRecordWith(
      bibIds = List(bibId)
    )
    val record2 = createSierraItemRecordWith(
      bibIds = List(bibId)
    )

    val sierraTransformable = SierraTransformable(sourceId = bibId)
    val result1 = ItemLinker.linkItemRecord(sierraTransformable, record1)
    val result2 = ItemLinker.linkItemRecord(result1, record2)

    result1.itemData(record1.id) shouldBe record1
    result2.itemData(record2.id) shouldBe record2
  }

  it("only merges item records with matching bib IDs") {
    val bibId = createSierraRecordNumberString
    val unrelatedBibId = createSierraRecordNumberString

    val record = createSierraItemRecordWith(
      bibIds = List(unrelatedBibId),
      unlinkedBibIds = List()
    )

    val sierraTransformable = SierraTransformable(sourceId = bibId)

    val caught = intercept[RuntimeException] {
      ItemLinker.linkItemRecord(sierraTransformable, record)
    }

    caught.getMessage shouldEqual s"Non-matching bib id $bibId in item bib List($unrelatedBibId)"
  }
}
