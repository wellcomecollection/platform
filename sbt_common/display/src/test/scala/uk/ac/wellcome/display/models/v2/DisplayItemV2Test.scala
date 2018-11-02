package uk.ac.wellcome.display.models.v2

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.ItemsGenerators

class DisplayItemV2Test extends FunSpec with Matchers with ItemsGenerators {

  it("reads an identified Item as a DisplayItemV2") {
    val item = createIdentifiedItem

    val displayItemV2 = DisplayItemV2(
      item = item,
      includesIdentifiers = true
    )

    displayItemV2.id shouldBe Some(item.canonicalId)
    displayItemV2.locations shouldBe List(
      DisplayLocationV2(item.agent.locations.head))
    displayItemV2.identifiers shouldBe Some(
      List(DisplayIdentifierV2(item.sourceIdentifier)))
    displayItemV2.ontologyType shouldBe "Item"
  }
  it("parses an unidentified Item as a DisplayItemV2") {
    val item = createUnidentifiableItemWith()

    val displayItemV2 = DisplayItemV2(
      item = item,
      includesIdentifiers = true
    )

    displayItemV2 shouldBe DisplayItemV2(
      id = None,
      identifiers = None,
      locations = List(DisplayLocationV2(item.agent.locations.head)))
  }

  it("parses an unidentified Item without any locations") {
    val item = createUnidentifiableItemWith(
      locations = List()
    )

    val displayItemV2 = DisplayItemV2(
      item = item,
      includesIdentifiers = true
    )

    displayItemV2.locations shouldBe List()
  }

  it("parses an Item without any extra identifiers") {
    val item = createIdentifiedItem

    val displayItemV2 = DisplayItemV2(
      item = item,
      includesIdentifiers = true
    )

    displayItemV2.identifiers shouldBe Some(
      List(DisplayIdentifierV2(item.sourceIdentifier)))
  }

  it("parses an identified Item without any locations") {
    val item = createIdentifiedItemWith(locations = List())

    val displayItemV2 = DisplayItemV2(
      item = item,
      includesIdentifiers = true
    )

    displayItemV2.locations shouldBe List()
  }
}
