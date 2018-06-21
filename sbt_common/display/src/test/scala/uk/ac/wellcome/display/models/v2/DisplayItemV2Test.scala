package uk.ac.wellcome.display.models.v2

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.utils.JsonUtil._

class DisplayItemV2Test extends FunSpec with Matchers {

  val location: Location = {
    val thumbnailUrl = "https://iiif.example.org/V0000001/default.jpg"
    val locationType = LocationType("thumbnail-image")

    DigitalLocation(
      locationType = locationType,
      url = thumbnailUrl,
      license = License_CCBY
    )
  }

  val identifier: SourceIdentifier = SourceIdentifier(
    identifierType = IdentifierType("miro-image-number"),
    ontologyType = "Item",
    value = "value"
  )

  it("should read an Item as a DisplayItemV2 correctly") {
    val item = IdentifiedItem(
      canonicalId = "foo",
      sourceIdentifier = identifier,
      locations = List(location)
    )

    val displayItemV2 = DisplayItemV2(
      item = item,
      includesIdentifiers = true
    )

    displayItemV2.id shouldBe item.canonicalId
    displayItemV2.locations shouldBe List(DisplayLocationV2(location))
    displayItemV2.identifiers shouldBe Some(
      List(DisplayIdentifierV2(identifier)))
    displayItemV2.ontologyType shouldBe "Item"
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

    val displayItemV2 = DisplayItemV2(
      item = item,
      includesIdentifiers = true
    )

    displayItemV2.identifiers shouldBe Some(List())
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

    val displayItemV2 = DisplayItemV2(
      item = item,
      includesIdentifiers = true
    )

    displayItemV2.locations shouldBe List()
  }
}
