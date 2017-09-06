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
    displayLocation.license shouldBe DisplayLicense(
      licenseType = License_CCBY.licenseType,
      label = License_CCBY.label,
      url = License_CCBY.url
    )
    displayLocation.ontologyType shouldBe "Location"
  }
}
