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
    it("reads a DigitalLocation as a DisplayDigitalLocationV2") {
      val thumbnailUrl = "https://iiif.example.org/V0000001/default.jpg"
      val locationType = LocationType("thumbnail-image")

      val internalLocation = DigitalLocation(
        locationType = locationType,
        url = thumbnailUrl,
        license = Some(License_CCBY)
      )
      val displayLocation = DisplayLocationV2(internalLocation)

      displayLocation shouldBe a[DisplayDigitalLocationV2]
      val displayDigitalLocation =
        displayLocation.asInstanceOf[DisplayDigitalLocationV2]
      displayDigitalLocation.locationType shouldBe DisplayLocationType(
        locationType)
      displayDigitalLocation.url shouldBe thumbnailUrl
      displayDigitalLocation.license shouldBe Some(
        DisplayLicenseV2(internalLocation.license.get))
      displayDigitalLocation.ontologyType shouldBe "DigitalLocation"
    }

    it("reads the credit field from a Location") {
      val location = DigitalLocation(
        locationType = LocationType("thumbnail-image"),
        url = "",
        credit = Some("Science Museum, Wellcome"),
        license = Some(License_CCBY)
      )
      val displayLocation = DisplayLocationV2(location)

      displayLocation shouldBe a[DisplayDigitalLocationV2]
      val displayDigitalLocation =
        displayLocation.asInstanceOf[DisplayDigitalLocationV2]
      displayDigitalLocation.credit shouldBe location.credit
    }
  }

  describe("DisplayPhysicalLocationV2") {
    it("creates a DisplayPhysicalLocationV2 from a PhysicalLocation") {
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

  describe("DisplayDigitalLocationV2") {
    it("creates a DisplayDigitalLocationV2 from a DigitalLocation") {
      val locationType = LocationType("iiif-image")
      val url = "https://wellcomelibrary.org/iiif/b2201508/manifest"

      val digitalLocation = DigitalLocation(url, locationType)

      DisplayLocationV2(digitalLocation) shouldBe DisplayDigitalLocationV2(
        locationType = DisplayLocationType(locationType),
        url = url)
    }
  }
}
