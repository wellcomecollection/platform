package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{
  DigitalLocation,
  LocationType,
  PhysicalLocation
}
import uk.ac.wellcome.platform.transformer.sierra.exceptions.SierraTransformerException
import uk.ac.wellcome.platform.transformer.sierra.source.sierra.SierraSourceLocation
import uk.ac.wellcome.platform.transformer.sierra.generators.SierraDataGenerators

class SierraLocationTest
    extends FunSpec
    with Matchers
    with SierraDataGenerators {

  private val transformer = new SierraLocation {}

  describe("Physical locations") {
    it("extracts location from item data") {
      val locationTypeCode = "sgmed"
      val locationType = LocationType("sgmed")
      val label = "A museum of mermaids"
      val itemData = createSierraItemDataWith(
        location = Some(SierraSourceLocation(locationTypeCode, label))
      )
      val expectedLocation = PhysicalLocation(locationType, label)

      transformer.getPhysicalLocation(itemData = itemData) shouldBe Some(
        expectedLocation)
    }

    it("returns None if the location field only contains empty strings") {
      val itemData = createSierraItemDataWith(
        location = Some(SierraSourceLocation("", ""))
      )

      transformer.getPhysicalLocation(itemData = itemData) shouldBe None
    }

    it("returns None if the location field only contains the string 'none'") {
      val itemData = createSierraItemDataWith(
        location = Some(SierraSourceLocation("none", "none"))
      )
      transformer.getPhysicalLocation(itemData = itemData) shouldBe None
    }

    it("returns None if there is no location in the item data") {
      val itemData = createSierraItemDataWith(
        location = None
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
        locationType = LocationType("iiif-presentation")
      )
      transformer.getDigitalLocation(id) shouldBe expectedLocation
    }

    it("throws an exception if no resource identifier is supplied") {
      val caught = intercept[SierraTransformerException] {
        transformer.getDigitalLocation(identifier = "")
      }
      caught.e.getMessage shouldEqual "id required by DigitalLocation has not been provided"
    }
  }
}
