package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.exceptions.TransformerException
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
        createSierraItemRecordWith(data = data)
      }.toList

      val transformable = createSierraTransformableWith(
        itemRecords = itemRecords
      )

      transformer
        .extractItemData(transformable)
        .values should contain theSameElementsAs itemData
    }

    it("throws an error if it gets some item data that isn't JSON") {
      val itemData = createSierraItemData
      val itemIdBad = createSierraItemNumber

      val notAJsonString = "<xml?>This is not a real 'JSON' string"

      val itemRecords = List(
        createSierraItemRecordWith(data = itemData),
        createSierraItemRecordWith(id = itemIdBad, data = notAJsonString)
      )

      val transformable = createSierraTransformableWith(
        itemRecords = itemRecords
      )

      val caught = intercept[TransformerException] {
        transformer.extractItemData(transformable)
      }

      caught.getMessage shouldBe s"Unable to parse item data for $itemIdBad as JSON: <<$notAJsonString>>"
    }
  }

  describe("transformItemData") {
    it("creates both forms of the Sierra ID in 'identifiers'") {
      val item = createSierraItemData
      val itemId = createSierraItemNumber

      val sourceIdentifier1 = createSierraSourceIdentifierWith(
        ontologyType = "Item",
        value = itemId.withCheckDigit)

      val sourceIdentifier2 = SourceIdentifier(
        identifierType = IdentifierType("sierra-identifier"),
        ontologyType = "Item",
        value = itemId.withoutCheckDigit
      )

      val expectedIdentifiers = List(sourceIdentifier1, sourceIdentifier2)

      val transformedItem = transformer.transformItemData(
        itemId = itemId,
        itemData = item
      )

      transformedItem shouldBe Identifiable(
        sourceIdentifier = sourceIdentifier1,
        otherIdentifiers = List(sourceIdentifier2),
        agent = Item(locations = List()))

      transformedItem.identifiers shouldBe expectedIdentifiers
    }

    it("uses the full Sierra system number as the source identifier") {
      val itemId = createSierraItemNumber
      val sourceIdentifier = createSierraSourceIdentifierWith(
        ontologyType = "Item",
        value = itemId.withCheckDigit
      )
      val sierraItemData = createSierraItemData

      val transformedItem = transformer.transformItemData(
        itemId = itemId,
        itemData = sierraItemData
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
          createSierraItemRecordWith(data = item1),
          createSierraItemRecordWith(data = item2)
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
              locationType = LocationType("iiif-presentation"))))
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
