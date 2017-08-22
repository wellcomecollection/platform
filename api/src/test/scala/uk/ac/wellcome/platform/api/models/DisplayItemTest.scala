package uk.ac.wellcome.platform.api.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._


class DisplayItemTest extends FunSpec with Matchers {

  val license: License = {
    val licenseType = "CC-Test"
    val label = "A fictional license for testing"
    val url = "http://creativecommons.org/licenses/test/-1.0/"

    License(
      licenseType = licenseType,
      label = label,
      url = url
    )
  }

  val location: Location = {
    val thumbnailUrl = Some("https://iiif.example.org/V0000001/default.jpg")
    val locationType = "thumbnail-image"

    val licenseType = "CC-Test"
    val licenseLabel = "A fictional license for testing"
    val licenseUrl = "http://creativecommons.org/licenses/test/-1.0/"

    Location(
      locationType = locationType,
      url = thumbnailUrl,
      license = license
    )
  }

  val identifier: SourceIdentifier = {
    val scheme = "identifier-scheme"
    val value = "value"

    SourceIdentifier(
      identifierScheme = scheme,
      value = value
    )
  }

  it("should read an Item as a DisplayItem correctly") {

    val item = Item(
        Some("foo"),
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

  }
}