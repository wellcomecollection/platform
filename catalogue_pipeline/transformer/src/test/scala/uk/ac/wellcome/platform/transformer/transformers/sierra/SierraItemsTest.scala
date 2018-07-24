package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{
  Identifiable,
  IdentifierType,
  Item,
  SourceIdentifier
}
import uk.ac.wellcome.platform.transformer.source.SierraItemData
import uk.ac.wellcome.platform.transformer.utils.SierraDataUtil

class SierraItemsTest extends FunSpec with Matchers with SierraDataUtil {

  val transformer = new Object with SierraItems

  describe("extractItemData") {
    it("parses instances of SierraItemData") {
      val itemData = (1 to 2).map { _ =>
        createSierraItemData
      }
      val itemRecords = itemData.map { data: SierraItemData =>
        createSierraItemRecordWith(data)
      }.toList

      val transformable = createSierraTransformableWith(
        itemRecords = itemRecords
      )

      transformer.extractItemData(transformable) shouldBe itemData
    }

    it("ignores items it can't parse as JSON") {
      val itemData = createSierraItemData

      val itemRecords = List(
        createSierraItemRecordWith(itemData),
        createSierraItemRecordWith(
          data = "<xml?>This is not a real 'JSON' string"
        )
      )

      val transformable = createSierraTransformableWith(
        itemRecords = itemRecords
      )

      transformer.extractItemData(transformable) shouldBe List(itemData)
    }
  }

  describe("transformItemData") {
    it("creates both forms of the Sierra ID in 'identifiers'") {
      val item = createSierraItemDataWith(
        id = "4000004"
      )

      val sourceIdentifier1 = SourceIdentifier(
        identifierType = IdentifierType("sierra-system-number"),
        ontologyType = "Item",
        value = "i40000047"
      )
      val sourceIdentifier2 = SourceIdentifier(
        identifierType = IdentifierType("sierra-identifier"),
        ontologyType = "Item",
        value = "4000004"
      )

      val expectedIdentifiers = List(sourceIdentifier1, sourceIdentifier2)
      val transformedItem = transformer.transformItemData(item)
      transformedItem shouldBe Identifiable(
        sourceIdentifier = sourceIdentifier1,
        otherIdentifiers = List(sourceIdentifier2),
        agent = Item(locations = List()))
      transformedItem.identifiers shouldBe expectedIdentifiers
    }

    it("uses the full Sierra system number as the source identifier") {
      val item = createSierraItemDataWith(id = "5000005")

      val sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("sierra-system-number"),
        ontologyType = "Item",
        value = "i50000056"
      )

      val transformedItem = transformer.transformItemData(item)
      transformedItem.sourceIdentifier shouldBe sourceIdentifier
    }
  }

  describe("getItems") {
    it("removes items with deleted=true") {
      val item1 = createSierraItemDataWith(deleted = true)
      val item2 = createSierraItemDataWith(deleted = false)

      val transformable = createSierraTransformableWith(
        itemRecords = List(
          createSierraItemRecordWith(item1),
          createSierraItemRecordWith(item2)
        )
      )

      transformer.getItems(transformable) should have size 1
    }
  }
}
