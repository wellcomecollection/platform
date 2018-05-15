package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.PhysicalLocation
import uk.ac.wellcome.platform.transformer.source.{SierraItemData, SierraItemLocation}

class SierraLocationTest extends FunSpec with Matchers {

  val transformer = new SierraLocation {}

  it("should extract location from item data") {
    val locationType = "sgmed"
    val label = "A museum of mermaids"
    val itemData = SierraItemData(
      id = "i1234567",
      location = Some(SierraItemLocation(locationType, label))
    )

    val expectedLocation = PhysicalLocation(locationType, label)

    transformer.getLocation(itemData = itemData) shouldBe Some(
      expectedLocation)
  }

  it("should return none if there is no location in the item data") {
    val itemData = SierraItemData(
      id = "i1234567"
    )

    transformer.getLocation(itemData = itemData) shouldBe None
  }
}
