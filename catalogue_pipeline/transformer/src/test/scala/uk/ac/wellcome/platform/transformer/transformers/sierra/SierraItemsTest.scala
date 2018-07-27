package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
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
        createSierraItemRecordWith(id = = createSierraRecordNumberString, data = data)
      }.toList

      val transformable = createSierraTransformableWith(
        itemRecords = itemRecords
      )

      transformer.extractItemData(transformable).values shouldBe itemData
    }

    it("ignores items it can't parse as JSON") {
      val itemData = createSierraItemData
      val itemId = createSierraRecordNumberString

      val itemRecords = List(
        createSierraItemRecordWith(id = itemId, data = itemData),
        createSierraItemRecordWith(
          data = "<xml?>This is not a real 'JSON' string"
        )
      )

      val transformable = createSierraTransformableWith(
        itemRecords = itemRecords
      )

      transformer.extractItemData(transformable) shouldBe Map(
        itemId -> itemData
      )
    }
  }

  describe("transformItemData") {
    it("creates both forms of the Sierra ID in 'identifiers'") {
      val item = createSierraItemData

      val sourceIdentifier1 = createSierraSourceIdentifierWith(
        ontologyType = "Item",
        value = "i40000047")

      val sourceIdentifier2 = SourceIdentifier(
        identifierType = IdentifierType("sierra-identifier"),
        ontologyType = "Item",
        value = "4000004"
      )

      val expectedIdentifiers = List(sourceIdentifier1, sourceIdentifier2)

      val transformedItem = transformer.transformItemData(
        itemId = "4000004", itemData = item
      )

      transformedItem shouldBe Identifiable(
        sourceIdentifier = sourceIdentifier1,
        otherIdentifiers = List(sourceIdentifier2),
        agent = Item(locations = List()))

      transformedItem.identifiers shouldBe expectedIdentifiers
    }

    it("uses the full Sierra system number as the source identifier") {
      val sourceIdentifier = createSierraSourceIdentifierWith(
        ontologyType = "Item",
        value = "i50000056"
      )
      val sierraItemData = createSierraItemData

      val transformedItem = transformer.transformItemData(
        itemId = "5000005", itemData = sierraItemData
      )
      transformedItem.sourceIdentifier shouldBe sourceIdentifier
    }
  }

  describe("getItems") {
    it("removes items with deleted=true") {
      val item1 = createSierraItemDataWith(deleted = true)
      val item2 = createSierraItemDataWith(deleted = false)

      val transformable = createSierraTransformableWith(
        itemRecords = List(
          createSierraItemRecordWith(id = createSierraRecordNumberString, data = item1),
          createSierraItemRecordWith(id = createSierraRecordNumberString, data = item2)
        )
      )

      transformer.getPhysicalItems(transformable) should have size 1
    }

    it("creates an DigitalLocation within items for an e-book record") {
      val sourceIdentifier = createSierraSourceIdentifier

      val bibData = createSierraBibDataWith(
        materialType = Some(createSierraEbookMaterialType)
      )

      val expectedItems = List(
        Identifiable(
          sourceIdentifier = sourceIdentifier,
          agent = Item(
            locations = List(DigitalLocation(
              url = s"https://wellcomelibrary.org/iiif/${sourceIdentifier.value}/manifest",
              license = None,
              locationType = LocationType("iiif-image"))))
        ))
      transformer.getDigitalItems(sourceIdentifier, bibData) shouldBe expectedItems
    }

    it("returns no DigitalItems if the work is not an eBook") {
      val sourceIdentifier = createSierraSourceIdentifier
      val bibData = createSierraBibDataWith(
        materialType =
          Some(createSierraMaterialTypeWith(code = "x", value = "book"))
      )

      transformer.getDigitalItems(sourceIdentifier, bibData) shouldBe List.empty
    }
  }
}
