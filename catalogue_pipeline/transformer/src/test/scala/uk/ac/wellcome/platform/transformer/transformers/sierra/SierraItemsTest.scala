package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraUtil
import uk.ac.wellcome.models.work.internal.{
  Identifiable,
  IdentifierType,
  Item,
  SourceIdentifier
}
import uk.ac.wellcome.platform.transformer.source.{SierraItemData, VarField}
import uk.ac.wellcome.utils.JsonUtil._

class SierraItemsTest extends FunSpec with Matchers with SierraUtil {

  val transformer = new Object with SierraItems

  describe("extractItemData") {
    it("parses instances of SierraItemData") {
      val bibId = "b1111111"
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
        item1.id -> createSierraItemRecordWith(
          id = item1.id,
          data = toJson(item1).get,
          bibIds = List(bibId)
        ),
        item2.id -> createSierraItemRecordWith(
          id = item2.id,
          data = toJson(item2).get,
          bibIds = List(bibId)
        )
      )

      val transformable = SierraTransformable(
        sourceId = bibId,
        itemData = itemData
      )

      transformer.extractItemData(transformable) shouldBe List(item1, item2)
    }

    it("ignores items it can't parse as JSON") {
      val bibId = "b2222222"
      val item = SierraItemData(
        id = "i2000001",
        deleted = true
      )

      val itemData = Map(
        item.id -> createSierraItemRecordWith(
          id = item.id,
          data = toJson(item).get,
          bibIds = List(bibId)
        ),
        "i2000002" -> createSierraItemRecordWith(
          id = "i2000002",
          data = "<xml?>This is not a real 'JSON' string",
          bibIds = List(bibId)
        )
      )

      val transformable = SierraTransformable(
        sourceId = bibId,
        itemData = itemData
      )

      transformer.extractItemData(transformable) shouldBe List(item)
    }
  }

  describe("transformItemData") {
    it("creates both forms of the Sierra ID in 'identifiers'") {
      val item = SierraItemData(id = "4000004", deleted = false)

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
      val item = SierraItemData(id = "5000005", deleted = false)

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
      val bibId = "3333333"
      val item1 = SierraItemData(id = "3000001", deleted = true)
      val item2 = SierraItemData(id = "3000002", deleted = false)

      val itemData = Map(
        item1.id -> createSierraItemRecordWith(
          id = item1.id,
          data = toJson(item1).get,
          bibIds = List(bibId)
        ),
        item2.id -> createSierraItemRecordWith(
          id = item2.id,
          data = toJson(item2).get,
          bibIds = List(bibId)
        )
      )

      val transformable = SierraTransformable(
        sourceId = bibId,
        itemData = itemData
      )

      transformer.getItems(transformable) should have size (1)
    }
  }
}
