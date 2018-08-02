package uk.ac.wellcome.platform.transformer.transformers.miro

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.DigitalLocation
import uk.ac.wellcome.platform.transformer.source.MiroTransformableData

class MiroItemsTest extends FunSpec with Matchers{
  val transformer = new MiroItems {}

  it("finds the credit line for an image-specific contributor code") {
    transformer.getItems(
      miroId = "B0011308",
      miroData = MiroTransformableData(
        creditLine = None,
        sourceCode = Some("FDN"),
        useRestrictions = Some("CC-0")
      )
    ).head.agent.locations.head.asInstanceOf[DigitalLocation].credit shouldBe Some("Ezra Feilden")
  }
}
