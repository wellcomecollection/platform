package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.PhysicalLocation
import uk.ac.wellcome.transformer.source.{SierraItemData, SierraLocation}

class SierraLocationTest extends FunSpec with Matchers {
  val transformer = new SierraLocation {}
  it("should add locations to items") {
    val locationType = "sgmed"
    val label = "A museum of mermaids"
    val itemData = SierraItemData(
      id = "i1234567",
      location = Some(SierraLocation(locationType, label))
    )

    val expectedLocation = PhysicalLocation(locationType, label)


    transformer.getLocation(itemData = itemData) shouldBe expectedLocation
  }
}

trait SierraLocation{
  def getLocation(itemData: SierraItemData): PhysicalLocation = ???
}
