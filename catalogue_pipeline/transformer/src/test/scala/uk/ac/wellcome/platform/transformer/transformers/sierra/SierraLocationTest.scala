package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{LocationType, PhysicalLocation}
import uk.ac.wellcome.platform.transformer.source.SierraItemLocation
import uk.ac.wellcome.platform.transformer.utils.SierraDataUtil

class SierraLocationTest extends FunSpec with Matchers with SierraDataUtil {

  val transformer = new SierraLocation {}

  it("extracts location from item data") {
    val locationTypeCode = "sgmed"
    val locationType = LocationType("sgmed")
    val label = "A museum of mermaids"
    val itemData = createSierraItemDataWith(
      location = Some(SierraItemLocation(locationTypeCode, label))
    )

    val expectedLocation = PhysicalLocation(locationType, label)

    transformer.getLocation(itemData = itemData) shouldBe Some(expectedLocation)
  }

  it("returns None if the location field only contains empty strings") {
    val itemData = createSierraItemDataWith(
      location = Some(SierraItemLocation("", ""))
    )

    transformer.getLocation(itemData = itemData) shouldBe None
  }

  it("returns None if the location field only contains the string 'none'") {
    val itemData = createSierraItemDataWith(
      location = Some(SierraItemLocation("none", "none"))
    )

    transformer.getLocation(itemData = itemData) shouldBe None
  }

  it("returns None if there is no location in the item data") {
    val itemData = createSierraItemDataWith(
      location = None
    )

    transformer.getLocation(itemData = itemData) shouldBe None
  }
}
