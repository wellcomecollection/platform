package uk.ac.wellcome.display.models.v2

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{
  DigitalLocation,
  License_CCBY,
  LocationType,
  PhysicalLocation
}

class DisplayLocationV2Test extends FunSpec with Matchers {

  describe("DisplayDigitalLocationV2") {
    it("should read a DigitalLocation as a DisplayDigitalLocationV2 correctly") {
      val thumbnailUrl = "https://iiif.example.org/V0000001/default.jpg"
      val locationType = LocationType("thumbnail-image")

      val internalLocation = DigitalLocation(
        locationType = locationType,
        url = thumbnailUrl,
        license = License_CCBY
      )
      val displayLocation = DisplayLocationV2(internalLocation)

      displayLocation shouldBe a[DisplayDigitalLocationV2]
      val displayDigitalLocation =
        displayLocation.asInstanceOf[DisplayDigitalLocationV2]
      displayDigitalLocation.locationType shouldBe DisplayLocationType(locationType)
      displayDigitalLocation.url shouldBe thumbnailUrl
      displayDigitalLocation.license shouldBe DisplayLicenseV2(
        internalLocation.license)
      displayDigitalLocation.ontologyType shouldBe "DigitalLocation"
    }

    it("should read the credit field from a Location correctly") {
      val location = DigitalLocation(
        locationType = LocationType("thumbnail-image"),
        url = "",
        credit = Some("Science Museum, Wellcome"),
        license = License_CCBY
      )
      val displayLocation = DisplayLocationV2(location)

      displayLocation shouldBe a[DisplayDigitalLocationV2]
      val displayDigitalLocation =
        displayLocation.asInstanceOf[DisplayDigitalLocationV2]
      displayDigitalLocation.credit shouldBe location.credit
    }
  }

  describe("DisplayPhysicalLocationV2") {
    it("should create a DisplayPhysicalLocationV2 from a PhysicalLocation") {
      val locationType = LocationType("sgmed")
      val locationLabel = "The collection of cold cauldrons"
      val physicalLocation =
        PhysicalLocation(locationType = locationType, label = locationLabel)

      val displayLocation = DisplayLocationV2(physicalLocation)

      displayLocation shouldBe DisplayPhysicalLocationV2(
        locationType = DisplayLocationType(locationType),
        locationLabel)
    }
  }
}
