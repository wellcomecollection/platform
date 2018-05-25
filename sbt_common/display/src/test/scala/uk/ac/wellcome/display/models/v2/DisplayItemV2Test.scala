package uk.ac.wellcome.display.models.v2

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.utils.JsonUtil._

class DisplayItemV2Test extends FunSpec with Matchers {

  val location: Location = {
    val thumbnailUrl = "https://iiif.example.org/V0000001/default.jpg"
    val locationType = "thumbnail-image"

    DigitalLocation(
      locationType = locationType,
      url = thumbnailUrl,
      license = License_CCBY
    )
  }

  val identifier: SourceIdentifier = SourceIdentifier(
    identifierType = IdentifierType("MiroImageNumber"),
    ontologyType = "Item",
    value = "value"
  )

  it("should read an Item as a DisplayItemV2 correctly") {
    val item = IdentifiedItem(
      canonicalId = "foo",
      sourceIdentifier = identifier,
      identifiers = List(identifier),
      locations = List(location)
    )

    val DisplayItemV2 = DisplayItemV2(
      item = item,
      includesIdentifiers = true
    )

    DisplayItemV2.id shouldBe item.canonicalId
    DisplayItemV2.locations shouldBe List(DisplayLocation(location))
    DisplayItemV2.identifiers shouldBe Some(List(DisplayIdentifier(identifier)))
    DisplayItemV2.ontologyType shouldBe "Item"
  }

  it("correctly parses an Item without any identifiers") {
    val item =
      fromJson[IdentifiedItem]("""
        {
          "canonicalId": "b71876a",
          "sourceIdentifier": {
            "identifierType": {
              "id": "miro-image-number",
              "label": "Miro image number",
              "ontologyType": "IdentifierType"
            },
            "ontologyType": "Item",
            "value": "B718760"
          },
          "locations": [],
          "type": "item"
        }
      """).get

    val DisplayItemV2 = DisplayItemV2(
      item = item,
      includesIdentifiers = true
    )

    DisplayItemV2.identifiers shouldBe Some(List())
  }

  it("correctly parses an Item without any locations") {
    val item =
      fromJson[IdentifiedItem]("""
        {
          "canonicalId": "mr953zsh",
          "sourceIdentifier": {
            "identifierType": {
              "id": "miro-image-number",
              "label": "Miro image number",
              "ontologyType": "IdentifierType"
            },
            "ontologyType": "Item",
            "value": "M9530000"
          },
          "identifiers": [],
          "type": "item"
        }
      """).get

    val DisplayItemV2 = DisplayItemV2(
      item = item,
      includesIdentifiers = true
    )

    DisplayItemV2.locations shouldBe List()
  }
}
