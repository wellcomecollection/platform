package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{LocationType, PhysicalLocation}
import uk.ac.wellcome.platform.transformer.source.SierraItemLocation
import uk.ac.wellcome.platform.transformer.utils.SierraDataUtil

class SierraLocationTest extends FunSpec with Matchers with SierraDataUtil {

  private val transformer = new SierraLocation {}

  describe("Physical locations") {
    it("extracts location from item data") {
      val locationTypeCode = "sgmed"
      val locationType = LocationType("sgmed")
      val label = "A museum of mermaids"
      val itemData = createSierraItemDataWith(
        location = Some(SierraItemLocation(locationTypeCode, label))
      )
      val expectedLocation = PhysicalLocation(locationType, label)

      transformer.getPhysicalLocation(itemData = itemData) shouldBe Some(
        expectedLocation)
    }

    it("returns None if the location field only contains empty strings") {
      val itemData = createSierraItemDataWith(
        location = Some(SierraItemLocation("", ""))
      )

      transformer.getPhysicalLocation(itemData = itemData) shouldBe None
    }

    it("returns None if the location field only contains the string 'none'") {
      val itemData = createSierraItemDataWith(
        location = Some(SierraItemLocation("none", "none"))
      )
      transformer.getPhysicalLocation(itemData = itemData) shouldBe None
    }
  }

  describe("Digital locations") {
    it("returns a digital location based on the id") {
      val id = "b2201508"
      val expectedLocation = DigitalLocation(
        url = "https://wellcomelibrary.org/iiif/b2201508/manifest",
        license = None,
        locationType = LocationType("iiif-image")
      )
      transformer.getDigitalLocation(id) shouldBe expectedLocation
    }

    it("throws an exception if no resource identifier is supplied") {
      val caught = intercept[RuntimeException] {
        transformer.getDigitalLocation(identifier = "")
      }
      caught.getMessage shouldEqual "id required by DigitalLocation has not been provided"
    }
  }
}
