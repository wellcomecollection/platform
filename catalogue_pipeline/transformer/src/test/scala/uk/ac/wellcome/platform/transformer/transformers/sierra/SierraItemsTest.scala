package uk.ac.wellcome.platform.transformer.transformers.sierra

import java.time.Instant

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.models.work.internal.{
  Identifiable,
  IdentifierType,
  Item,
  SourceIdentifier
}
import uk.ac.wellcome.platform.transformer.utils.SierraDataUtil
import uk.ac.wellcome.utils.JsonUtil._

class SierraItemsTest extends FunSpec with Matchers with SierraDataUtil {

  val transformer = new Object with SierraItems

  describe("extractItemData") {
    it("parses instances of SierraItemData") {
      val item1 = createSierraItemData
      val item2 = createSierraItemData

      val itemData = Map(
        item1.id -> SierraItemRecord(
          id = item1.id,
          data = toJson(item1).get,
          modifiedDate = Instant.now,
          bibIds = List()
        ),
        item2.id -> SierraItemRecord(
          id = item2.id,
          data = toJson(item2).get,
          modifiedDate = Instant.now,
          bibIds = List()
        )
      )

      val transformable = SierraTransformable(
        sourceId = createSierraRecordNumberString,
        itemData = itemData
      )

      transformer.extractItemData(transformable) shouldBe List(item1, item2)
    }

    it("ignores items it can't parse as JSON") {
      val item = createSierraItemData

      val itemId = createSierraRecordNumberString

      val itemData = Map(
        item.id -> SierraItemRecord(
          id = item.id,
          data = toJson(item).get,
          modifiedDate = Instant.now,
          bibIds = List()
        ),
        itemId -> SierraItemRecord(
          id = itemId,
          data = "<xml?>This is not a real 'JSON' string",
          modifiedDate = Instant.now,
          bibIds = List()
        )
      )

      val transformable = SierraTransformable(
        sourceId = createSierraRecordNumberString,
        itemData = itemData
      )

      transformer.extractItemData(transformable) shouldBe List(item)
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

      val itemData = Map(
        item1.id -> SierraItemRecord(
          id = item1.id,
          data = toJson(item1).get,
          modifiedDate = Instant.now,
          bibIds = List()
        ),
        item2.id -> SierraItemRecord(
          id = item2.id,
          data = toJson(item2).get,
          modifiedDate = Instant.now,
          bibIds = List()
        )
      )

      val transformable = SierraTransformable(
        sourceId = createSierraRecordNumberString,
        itemData = itemData
      )

      transformer.getPhysicalItems(transformable) should have size (1)
    }

    it("creates an DigitalLocation within items for an e-book record") {
      val id = "b1234567"
      val sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("sierra-system-number"),
        ontologyType = "Item",
        value = id
      )

      val bibData = SierraBibData(
        id = id,
        title = Some("title"),
        materialType = Some(
          SierraMaterialType(
            code = "v",
            value = "E-books"
          ))
      )

      val expectedLocations = List(Identifiable(
        sourceIdentifier = sourceIdentifier,
        agent = Item(locations = List(DigitalLocation(
          url = s"https://wellcomelibrary.org/iiif/$id/manifest",
          license = None,
          locationType = LocationType("iiif-image"))))))
      transformer.getDigitalItems(sourceIdentifier, bibData) shouldBe expectedLocations
    }

    it("returns no DigitalItems if the work is not an eBook") {
      val id = "b1234567"
      val sourceIdentifier = SourceIdentifier(
        identifierType = IdentifierType("sierra-system-number"),
        ontologyType = "Item",
        value = id
      )

      val sierraBibData = SierraBibData(id)

      transformer.getDigitalItems(sourceIdentifier, sierraBibData) shouldBe List.empty
    }
  }
}
