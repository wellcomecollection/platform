package uk.ac.wellcome.platform.sierra_item_merger.links

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.test.utils.{SierraData, SierraUtil}

class ItemUnlinkerTests extends FunSpec with Matchers with SierraData with SierraUtil {

  it("removes the item if it already exists") {
    val bibId = "222"

    val record = createSierraItemRecordWith(
      bibIds = List(bibId)
    )

    val unlinkedItemRecord = record.copy(
      bibIds = Nil,
      modifiedDate = record.modifiedDate.plusSeconds(1),
      unlinkedBibIds = List(bibId)
    )

    val sierraTransformable = SierraTransformable(
      sourceId = bibId,
      itemData = Map(record.id -> record)
    )

    val expectedSierraTransformable = sierraTransformable.copy(
      itemData = Map.empty
    )

    ItemUnlinker.unlinkItemRecord(sierraTransformable, unlinkedItemRecord) shouldBe expectedSierraTransformable
  }

  it(
    "returns the original record when merging an unlinked record which is already absent") {
    val bibId = "333"

    val record = createSierraItemRecordWith(
      bibIds = List(bibId)
    )

    val previouslyUnlinkedRecord = createSierraItemRecordWith(
      id = record.id,
      unlinkedBibIds = List(bibId)
    )

    val sierraTransformable = SierraTransformable(
      sourceId = bibId,
      itemData = Map(record.id -> record)
    )

    val expectedSierraTransformable = sierraTransformable

    ItemUnlinker.unlinkItemRecord(sierraTransformable, previouslyUnlinkedRecord) shouldBe expectedSierraTransformable
  }

  it(
    "returns the original record when merging an unlinked record which has linked more recently") {
    val bibId = "444"
    val itemId = "i111"

    val record = createSierraItemRecordWith(
      modifiedDate = newerDate,
      bibIds = List(bibId)
    )

    val outOfDateUnlinkedRecord = createSierraItemRecordWith(
      id = record.id,
      modifiedDate = olderDate,
      unlinkedBibIds = List(bibId)
    )

    val sierraTransformable = SierraTransformable(
      sourceId = bibId,
      itemData = Map(record.id -> record)
    )

    val expectedSierraTransformable = sierraTransformable

    ItemUnlinker.unlinkItemRecord(sierraTransformable, outOfDateUnlinkedRecord) shouldBe expectedSierraTransformable
  }

  it("should only unlink item records with matching bib IDs") {
    val bibId = "222"
    val unrelatedBibId = "846"

    val record = createSierraItemRecordWith(
      bibIds = List(bibId)
    )

    val unrelatedItemRecord = createSierraItemRecordWith(
      unlinkedBibIds = List(unrelatedBibId)
    )

    val sierraTransformable = SierraTransformable(
      sourceId = bibId,
      itemData = Map(record.id -> record)
    )

    val caught = intercept[RuntimeException] {
      ItemUnlinker.unlinkItemRecord(sierraTransformable, unrelatedItemRecord)
    }

    caught.getMessage shouldEqual s"Non-matching bib id $bibId in item unlink bibs List($unrelatedBibId)"
  }
}
