package uk.ac.wellcome.display.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{DigitalLocation, License_CCBY, PhysicalLocation}

class DisplayLocationTest extends FunSpec with Matchers {

  describe("DisplayDigitalLocation") {
    it("should read a DigitalLocation as a DisplayDigitalLocation correctly") {
      val thumbnailUrl = "https://iiif.example.org/V0000001/default.jpg"
      val locationType = "thumbnail-image"

      val internalLocation = DigitalLocation(
        locationType = locationType,
        url = thumbnailUrl,
        license = License_CCBY
      )
      val displayLocation = DisplayLocation(internalLocation)

      displayLocation shouldBe a[DisplayDigitalLocation]
      val displayDigitalLocation =
        displayLocation.asInstanceOf[DisplayDigitalLocation]
      displayDigitalLocation.locationType shouldBe locationType
      displayDigitalLocation.url shouldBe thumbnailUrl
      displayDigitalLocation.license shouldBe DisplayLicense(
        internalLocation.license)
      displayDigitalLocation.ontologyType shouldBe "DigitalLocation"
    }

    it("should read the credit field from a Location correctly") {
      val location = DigitalLocation(
        locationType = "thumbnail-image",
        url = "",
        credit = Some("Science Museum, Wellcome"),
        license = License_CCBY
      )
      val displayLocation = DisplayLocation(location)

      displayLocation shouldBe a[DisplayDigitalLocation]
      val displayDigitalLocation =
        displayLocation.asInstanceOf[DisplayDigitalLocation]
      displayDigitalLocation.credit shouldBe location.credit
    }
  }

  describe("DisplayPhysicalLocation") {
    it("should create a DisplayPhysicalLocation from a PhysicalLocation") {
      val locationType = "sgmed"
      val locationLabel = "The collection of cold cauldrons"
      val physicalLocation =
        PhysicalLocation(locationType = locationType, label = locationLabel)

      val displayLocation = DisplayLocation(physicalLocation)

      displayLocation shouldBe DisplayPhysicalLocation(
        locationType,
        locationLabel)
    }
  }
}
