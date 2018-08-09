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
    it("reads a DigitalLocation as a DisplayDigitalLocationV1") {
      val thumbnailUrl = "https://iiif.example.org/V0000001/default.jpg"
      val locationType = LocationType("thumbnail-image")

      val internalLocation = DigitalLocation(
        locationType = locationType,
        url = thumbnailUrl,
        license = Some(License_CCBY)
      )
      val displayLocation = DisplayLocationV1(internalLocation)

      displayLocation shouldBe a[DisplayDigitalLocationV1]
      val displayDigitalLocation =
        displayLocation.asInstanceOf[DisplayDigitalLocationV1]
      displayDigitalLocation.locationType shouldBe locationType.id
      displayDigitalLocation.url shouldBe thumbnailUrl
      displayDigitalLocation.license shouldBe Some(
        DisplayLicenseV1(internalLocation.license.get))
      displayDigitalLocation.ontologyType shouldBe "DigitalLocation"
    }

    it("reads the credit field from a Location") {
      val location = DigitalLocation(
        locationType = LocationType("thumbnail-image"),
        url = "",
        credit = Some("Science Museum, Wellcome"),
        license = Some(License_CCBY)
      )
      val displayLocation = DisplayLocationV1(location)

      displayLocation shouldBe a[DisplayDigitalLocationV1]
      val displayDigitalLocation =
        displayLocation.asInstanceOf[DisplayDigitalLocationV1]
      displayDigitalLocation.credit shouldBe location.credit
    }
  }

  describe("DisplayPhysicalLocationV1") {
    it("creates a DisplayPhysicalLocationV1 from a PhysicalLocation") {
      val locationType = LocationType("sgmed")
      val locationLabel = "The collection of cold cauldrons"
      val physicalLocation =
        PhysicalLocation(locationType = locationType, label = locationLabel)

      val displayLocation = DisplayLocationV1(physicalLocation)

      displayLocation shouldBe DisplayPhysicalLocationV1(
        locationType = locationType.id,
        locationLabel)
    }
  }

  describe("DisplayDigitalLocationV1") {
    it("creates a DisplayDigitalLocationV1 from a DigitalLocation") {
      val locationType = LocationType("iiif-image")
      val url = "https://wellcomelibrary.org/iiif/b2201508/manifest"

      val digitalLocation = DigitalLocation(url, locationType)

      DisplayLocationV1(digitalLocation) shouldBe DisplayDigitalLocationV1(
        locationType = locationType.id,
        url = url
      )
    }

  }
}
