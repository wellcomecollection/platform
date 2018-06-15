package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{LocationType, PhysicalLocation}
import uk.ac.wellcome.platform.transformer.source.{
  SierraItemData,
  SierraItemLocation
}

class SierraLocationTest extends FunSpec with Matchers {

  val transformer = new SierraLocation {}

  it("extracts location from item data") {
    val locationTypeCode = "sgmed"
    val locationType = LocationType("sgmed")
    val label = "A museum of mermaids"
    val itemData = SierraItemData(
      id = "i1234567",
      location = Some(SierraItemLocation(locationTypeCode, label))
    )

    val expectedLocation = PhysicalLocation(locationType, label)

    transformer.getLocation(itemData = itemData) shouldBe Some(
      expectedLocation)
  }

  it("returns None if there is no location in the item data") {
    val itemData = SierraItemData(
      id = "i1234567"
    )

    transformer.getLocation(itemData = itemData) shouldBe None
  }
}
