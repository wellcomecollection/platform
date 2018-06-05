package uk.ac.wellcome.display.models.v1

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{
  DigitalLocation,
  License_CCBY,
  LocationType,
  PhysicalLocation
}

class DisplayLocationV1Test extends FunSpec with Matchers {

  describe("DisplayDigitalLocationV1") {
    it("should read a DigitalLocation as a DisplayDigitalLocationV1 correctly") {
      val thumbnailUrl = "https://iiif.example.org/V0000001/default.jpg"
      val locationType = LocationType("thumbnail-image")

      val internalLocation = DigitalLocation(
        locationType = locationType,
        url = thumbnailUrl,
        license = License_CCBY
      )
      val displayLocation = DisplayLocationV1(internalLocation)

      displayLocation shouldBe a[DisplayDigitalLocationV1]
      val displayDigitalLocation =
        displayLocation.asInstanceOf[DisplayDigitalLocationV1]
      displayDigitalLocation.locationType shouldBe locationType
      displayDigitalLocation.url shouldBe thumbnailUrl
      displayDigitalLocation.license shouldBe DisplayLicenseV1(
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
      val displayLocation = DisplayLocationV1(location)

      displayLocation shouldBe a[DisplayDigitalLocationV1]
      val displayDigitalLocation =
        displayLocation.asInstanceOf[DisplayDigitalLocationV1]
      displayDigitalLocation.credit shouldBe location.credit
    }
  }

  describe("DisplayPhysicalLocationV1") {
    it("should create a DisplayPhysicalLocationV1 from a PhysicalLocation") {
      val locationType = "sgmed"
      val locationLabel = "The collection of cold cauldrons"
      val physicalLocation =
        PhysicalLocation(locationType = locationType, label = locationLabel)

      val displayLocation = DisplayLocationV1(physicalLocation)

      displayLocation shouldBe DisplayPhysicalLocationV1(
        locationType,
        locationLabel)
    }
  }
}
