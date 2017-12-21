package uk.ac.wellcome.platform.api.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._

class DisplayItemTest extends FunSpec with Matchers {

  val location: Location = {
    val thumbnailUrl = Some("https://iiif.example.org/V0000001/default.jpg")
    val locationType = "thumbnail-image"

    Location(
      locationType = locationType,
      url = thumbnailUrl,
      license = License_CCBY
    )
  }

  val identifier: SourceIdentifier = SourceIdentifier(
    identifierScheme = IdentifierSchemes.miroImageNumber,
    value = "value"
  )

  it("should read an Item as a DisplayItem correctly") {
    val item = Item(
      canonicalId = Some("foo"),
      sourceIdentifier = identifier,
      identifiers = List(identifier),
      locations = List(location)
    )

    val displayItem = DisplayItem(
      item = item,
      includesIdentifiers = true
    )

    displayItem.id shouldBe item.id
    displayItem.locations shouldBe List(DisplayLocation(location))
    displayItem.identifiers shouldBe Some(List(DisplayIdentifier(identifier)))
    displayItem.ontologyType shouldBe "Item"
  }

  it("correctly parses an Item without any identifiers") {
    val item = Item(
      canonicalId = Some("b71876a"),
      sourceIdentifier = identifier,
      locations = List()
    )

    val displayItem = DisplayItem(
      item = item,
      includesIdentifiers = true
    )

    displayItem.identifiers shouldBe Some(List())
  }
}
