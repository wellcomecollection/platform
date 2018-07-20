package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.work.internal.{
  Identifiable,
  IdentifierType,
  Item,
  SourceIdentifier
}
import uk.ac.wellcome.platform.transformer.utils.SierraDataUtil

class SierraItemsTest extends FunSpec with Matchers with SierraDataUtil {

  val transformer = new Object with SierraItems

  describe("extractItemData") {
    it("parses instances of SierraItemData") {
      val data1 = createSierraItemDataWith(deleted = true)
      val data2 = createSierraItemDataWith(deleted = false)

      val itemData = Map(
        data1.id -> createSierraItemRecordWith(data1),
        data2.id -> createSierraItemRecordWith(data2)
      )

      val transformable = SierraTransformable(
        sierraId = createSierraRecordNumber,
        itemRecords = itemData
      )

      transformer.extractItemData(transformable) shouldBe List(data1, data2)
    }

    it("ignores items it can't parse as JSON") {
      val data = createSierraItemDataWith(deleted = true)

      val otherId = createSierraRecordNumber

      val itemData = Map(
        data.id -> createSierraItemRecordWith(data),
        otherId -> createSierraItemRecordWith(
          id = otherId.withoutCheckDigit,
          data = "<xml?>This is not a real 'JSON' string"
        )
      )

      val transformable = SierraTransformable(
        sierraId = createSierraRecordNumber,
        itemRecords = itemData
      )

      transformer.extractItemData(transformable) shouldBe List(data)
    }
  }

  describe("transformItemData") {
    it("creates both forms of the Sierra ID in 'identifiers'") {
      val data = createSierraItemDataWith(id = "4000004")

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
      val transformedItem = transformer.transformItemData(data)
      transformedItem shouldBe Identifiable(
        sourceIdentifier = sourceIdentifier1,
        otherIdentifiers = List(sourceIdentifier2),
        agent = Item(locations = List()))
      transformedItem.identifiers shouldBe expectedIdentifiers
    }

    it("uses the full Sierra system number as the source identifier") {
      val data = createSierraItemDataWith(id = "5000005")

      val sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("sierra-system-number"),
        ontologyType = "Item",
        value = "i50000056"
      )

      val transformedItem = transformer.transformItemData(data)
      transformedItem.sourceIdentifier shouldBe sourceIdentifier
    }
  }

  describe("getItems") {
    it("removes items with deleted=true") {
      val data1 = createSierraItemDataWith(deleted = true)
      val data2 = createSierraItemDataWith(deleted = false)

      val itemData = Map(
        data1.id -> createSierraItemRecordWith(data1),
        data2.id -> createSierraItemRecordWith(data2)
      )

      val transformable = SierraTransformable(
        sierraId = createSierraRecordNumber,
        itemRecords = itemData
      )

      transformer.getItems(transformable) should have size 1
    }
  }
}
