package uk.ac.wellcome.display.models.v1

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.test.util.ItemsUtil

class DisplayItemV1Test extends FunSpec with Matchers with ItemsUtil {

  it("should read an Item as a DisplayItemV1 correctly") {
    val item = createItem()

    val displayItemV1 = DisplayItemV1(
      item = item,
      includesIdentifiers = true
    )

    displayItemV1.id shouldBe item.canonicalId
    displayItemV1.locations shouldBe List(DisplayLocationV1(item.agent.locations.head))
    displayItemV1.identifiers shouldBe Some(
      List(DisplayIdentifierV1(item.sourceIdentifier)))
    displayItemV1.ontologyType shouldBe "Item"
  }

  it("correctly parses an Item without any extra identifiers") {
    val item = createItem()

    val displayItemV1 = DisplayItemV1(
      item = item,
      includesIdentifiers = true
    )

    displayItemV1.identifiers shouldBe Some(
      List(DisplayIdentifierV1(item.sourceIdentifier)))
  }

  it("correctly parses an Item without any locations") {
    val item = createItem(locations = List())

    val displayItemV1 = DisplayItemV1(
      item = item,
      includesIdentifiers = true
    )

    displayItemV1.locations shouldBe List()
  }
}
