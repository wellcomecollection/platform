package uk.ac.wellcome.platform.api.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._

class DisplayLocationTest extends FunSpec with Matchers {

  it("should read a Location as a DisplayLocation correctly") {
    val thumbnailUrl = Some("https://iiif.example.org/V0000001/default.jpg")
    val locationType = "thumbnail-image"

    val internalLocation = Location(
      locationType = locationType,
      url = thumbnailUrl,
      license = License_CCBY
    )
    val displayLocation = DisplayLocation(internalLocation)

    displayLocation.locationType shouldBe locationType
    displayLocation.url shouldBe thumbnailUrl
    displayLocation.license shouldBe DisplayLicense(internalLocation.license)
    displayLocation.ontologyType shouldBe "Location"
  }

  it("should read the copyright field from a Location correctly") {
    val location = Location(
      locationType = "thumbnail-image",
      copyright = Some("Science Museum, Wellcome"),
      license = License_CCBY
    )
    val displayLocation = DisplayLocation(location)

    displayLocation.copyright shouldBe location.copyright
  }
}
