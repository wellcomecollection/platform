package uk.ac.wellcome.platform.sierra_item_merger.links

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.test.utils.SierraData

class ItemUnlinkerTests extends FunSpec with Matchers with SierraData {

  it("removes the item if it already exists") {
    val bibId = "222"

    val record = sierraItemRecord(
      id = "i111",
      title = "Only otters occupy the orange oval",
      modifiedDate = "2001-01-01T01:01:01Z",
      bibIds = List(bibId),
      unlinkedBibIds = List()
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

    val record = sierraItemRecord(
      id = "i111",
      title = "Only otters occupy the orange oval",
      modifiedDate = "2001-01-01T01:01:01Z",
      bibIds = List(bibId),
      unlinkedBibIds = List()
    )

    val previouslyUnlinkedRecord = sierraItemRecord(
      id = "i222",
      title = "Only otters occupy the orange oval",
      modifiedDate = "2001-01-01T01:01:01Z",
      bibIds = List(),
      unlinkedBibIds = List(bibId)
    )

    val sierraTransformable = SierraTransformable(
      sourceId = bibId,
      itemData = Map(record.id -> record)
    )

    val expectedSierraTransformable = sierraTransformable

    ItemUnlinker.unlinkItemRecord(
      sierraTransformable,
      previouslyUnlinkedRecord) shouldBe expectedSierraTransformable
  }

  it(
    "returns the original record when merging an unlinked record which has linked more recently") {
    val bibId = "444"
    val itemId = "i111"

    val record = sierraItemRecord(
      id = itemId,
      title = "Only otters occupy the orange oval",
      modifiedDate = "2001-01-01T01:01:01Z",
      bibIds = List(bibId),
      unlinkedBibIds = List()
    )

    val outOfDateUnlinkedRecord = sierraItemRecord(
      id = record.id,
      title = "Curious clams caught in caul",
      modifiedDate = "2000-01-01T01:01:01Z",
      bibIds = List(),
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

    val record = sierraItemRecord(
      id = "i111",
      title = "Only otters occupy the orange oval",
      modifiedDate = "2001-01-01T01:01:01Z",
      bibIds = List(bibId),
      unlinkedBibIds = List()
    )

    val unrelatedItemRecord = sierraItemRecord(
      id = "i999",
      title = "Only otters occupy the orange oval",
      modifiedDate = "2001-01-01T01:01:01Z",
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

    caught.getMessage shouldEqual "Non-matching bib id 222 in item unlink bibs List(846)"
  }
}
