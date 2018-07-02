package uk.ac.wellcome.display.models.v2

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.test.util.ItemsUtil

class DisplayItemV2Test extends FunSpec with Matchers with ItemsUtil {

  it("should read an Item as a DisplayItemV2 correctly") {
    val item = createItem()

    val displayItemV2 = DisplayItemV2(
      item = item,
      includesIdentifiers = true
    )

    displayItemV2.id shouldBe item.canonicalId
    displayItemV2.locations shouldBe List(DisplayLocationV2(item.agent.locations.head))
    displayItemV2.identifiers shouldBe Some(
      List(DisplayIdentifierV2(item.sourceIdentifier)))
    displayItemV2.ontologyType shouldBe "Item"
  }

  it("correctly parses an Item without any extra identifiers") {
    val item = createItem()

    val displayItemV2 = DisplayItemV2(
      item = item,
      includesIdentifiers = true
    )

    displayItemV2.identifiers shouldBe Some(
      List(DisplayIdentifierV2(item.sourceIdentifier)))
  }

  it("correctly parses an Item without any locations") {
    val item = createItem(
      locations = List()
    )

    val displayItemV2 = DisplayItemV2(
      item = item,
      includesIdentifiers = true
    )

    displayItemV2.locations shouldBe List()
  }
}
