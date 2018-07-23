package uk.ac.wellcome.platform.sierra_item_merger.links

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil

class ItemUnlinkerTests extends FunSpec with Matchers with SierraUtil {

  it("removes the item if it already exists") {
    val bibId = createSierraRecordNumberString

    val record = createSierraItemRecordWith(
      bibIds = List(bibId),
      unlinkedBibIds = List()
    )

    val unlinkedItemRecord = record.copy(
      bibIds = List(),
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
    val bibId = createSierraRecordNumberString

    val record = createSierraItemRecordWith(
      bibIds = List(bibId),
      unlinkedBibIds = List()
    )

    val previouslyUnlinkedRecord = createSierraItemRecordWith(
      bibIds = List(),
      unlinkedBibIds = List(bibId)
    )

    val sierraTransformable = SierraTransformable(
      sourceId = bibId,
      itemData = Map(record.id -> record)
    )

    ItemUnlinker.unlinkItemRecord(sierraTransformable, previouslyUnlinkedRecord) shouldBe sierraTransformable
  }

  it(
    "returns the original record when merging an unlinked record which has linked more recently") {
    val bibId = createSierraRecordNumberString

    val record = createSierraItemRecordWith(
      modifiedDate = newerDate,
      bibIds = List(bibId),
      unlinkedBibIds = List()
    )

    val outOfDateUnlinkedRecord = record.copy(
      modifiedDate = olderDate,
      bibIds = List(),
      unlinkedBibIds = List(bibId)
    )

    val sierraTransformable = SierraTransformable(
      sourceId = bibId,
      itemData = Map(record.id -> record)
    )

    ItemUnlinker.unlinkItemRecord(sierraTransformable, outOfDateUnlinkedRecord) shouldBe sierraTransformable
  }

  it("should only unlink item records with matching bib IDs") {
    val bibId = createSierraRecordNumberString
    val unrelatedBibId = createSierraRecordNumberString

    val record = createSierraItemRecordWith(
      bibIds = List(bibId),
      unlinkedBibIds = List()
    )

    val unrelatedItemRecord = createSierraItemRecordWith(
      bibIds = List(),
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
