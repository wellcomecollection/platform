package uk.ac.wellcome.transformer.transformers.sierra

import java.time.Instant

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.models.work.internal.{IdentifierSchemes, SourceIdentifier, UnidentifiedItem}
import uk.ac.wellcome.test.utils.SierraData
import uk.ac.wellcome.transformer.source.{SierraItemData, VarField}
import uk.ac.wellcome.utils.JsonUtil._

class SierraItemsTest extends FunSpec with Matchers with SierraData {

  val transformer = new Object with SierraItems

  describe("extractItemData") {
    it("parses instances of SierraItemData") {
      val item1 = SierraItemData(
        id = "i1000001",
        deleted = true
      )

      val item2 = SierraItemData(
        id = "i1000002",
        deleted = false,
        varFields = List(
          VarField(fieldTag = "b", content = "X111658"),
          VarField(fieldTag = "c", content = "X111659")
        )
      )

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
        sourceId = "b1111111",
        itemData = itemData
      )

      transformer.extractItemData(transformable) shouldBe List(item1, item2)
    }

    it("ignores items it can't parse as JSON") {
      val item = SierraItemData(
        id = "i2000001",
        deleted = true
      )

      val itemData = Map(
        item.id -> SierraItemRecord(
          id = item.id,
          data = toJson(item).get,
          modifiedDate = Instant.now,
          bibIds = List()
        ),
        "i2000002" -> SierraItemRecord(
          id = "i2000002",
          data = "<xml?>This is not a real 'JSON' string",
          modifiedDate = Instant.now,
          bibIds = List()
        )
      )

      val transformable = SierraTransformable(
        sourceId = "b2222222",
        itemData = itemData
      )

      transformer.extractItemData(transformable) shouldBe List(item)
    }
  }

  describe("transformItemData") {
    it("returns UnidentifiedItem if an item is not deleted") {
      val item = SierraItemData(id = "4000002", deleted = false)
      transformer.transformItemData(item) shouldBe a[UnidentifiedItem]
    }

    it("creates both forms of the Sierra ID in 'identifiers'") {
      val item = SierraItemData(id = "4000004", deleted = false)

      val sourceIdentifier1 = SourceIdentifier(
        identifierScheme = IdentifierSchemes.sierraSystemNumber,
        ontologyType = "Item",
        value = "i40000047"
      )
      val sourceIdentifier2 = SourceIdentifier(
        identifierScheme = IdentifierSchemes.sierraIdentifier,
        ontologyType = "Item",
        value = "4000004"
      )

      val expectedIdentifiers = List(sourceIdentifier1, sourceIdentifier2)
      val transformedItem = transformer.transformItemData(item)
      transformedItem.identifiers shouldBe expectedIdentifiers
    }

    it("uses the full Sierra system number as the source identifier") {
      val item = SierraItemData(id = "5000005", deleted = false)

      val sourceIdentifier = SourceIdentifier(
        identifierScheme = IdentifierSchemes.sierraSystemNumber,
        ontologyType = "Item",
        value = "i50000056"
      )

      val transformedItem = transformer.transformItemData(item)
      transformedItem.sourceIdentifier shouldBe sourceIdentifier
    }
  }

  describe("getItems") {
    it("removes items with deleted=true") {
      val item1 = SierraItemData(id = "3000001", deleted = true)
      val item2 = SierraItemData(id = "3000002", deleted = false)

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
        sourceId = "3333333",
        itemData = itemData
      )

      transformer.getItems(transformable) should have size (1)
    }
  }
}
