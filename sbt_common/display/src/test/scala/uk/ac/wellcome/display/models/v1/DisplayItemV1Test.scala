package uk.ac.wellcome.display.models.v1

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.display.models.DisplayLocation
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.utils.JsonUtil._

class DisplayItemV1Test extends FunSpec with Matchers {

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

  it("should read an Item as a DisplayItemV1 correctly") {
    val item = IdentifiedItem(
      canonicalId = "foo",
      sourceIdentifier = identifier,
      identifiers = List(identifier),
      locations = List(location)
    )

    val displayItemV1 = DisplayItemV1(
      item = item,
      includesIdentifiers = true
    )

    displayItemV1.id shouldBe item.canonicalId
    displayItemV1.locations shouldBe List(DisplayLocation(location))
    displayItemV1.identifiers shouldBe Some(List(DisplayIdentifierV1(identifier)))
    displayItemV1.ontologyType shouldBe "Item"
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

    val displayItemV1 = DisplayItemV1(
      item = item,
      includesIdentifiers = true
    )

    displayItemV1.identifiers shouldBe Some(List())
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

    val displayItemV1 = DisplayItemV1(
      item = item,
      includesIdentifiers = true
    )

    displayItemV1.locations shouldBe List()
  }
}
