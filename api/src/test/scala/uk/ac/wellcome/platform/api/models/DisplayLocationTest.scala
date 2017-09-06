package uk.ac.wellcome.platform.api.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._

class DisplayLocationTest extends FunSpec with Matchers {

  it("should read a Location as a DisplayLocation correctly") {
    val thumbnailUrl = Some("https://iiif.example.org/V0000001/default.jpg")
    val locationType = "thumbnail-image"
    val licenseType = "CC-Test"
    val licenseLabel = "A fictional license for testing"
    val licenseUrl = "http://creativecommons.org/licenses/test/-1.0/"

    val internalLocation = Location(
      locationType = locationType,
      url = thumbnailUrl,
      license = License_CCBY
    )
    val displayLocation = DisplayLocation(internalLocation)

    displayLocation.locationType shouldBe locationType
    displayLocation.url shouldBe thumbnailUrl
    displayLocation.license shouldBe DisplayLicense(
      licenseType = licenseType,
      label = licenseLabel,
      url = licenseUrl
    )
    displayLocation.ontologyType shouldBe "Location"
  }
}
