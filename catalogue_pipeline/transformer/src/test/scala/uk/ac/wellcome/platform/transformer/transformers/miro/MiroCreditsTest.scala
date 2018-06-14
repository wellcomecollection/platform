package uk.ac.wellcome.platform.transformer.transformers.miro

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.transformer.source.MiroTransformableData

class MiroCreditsTest extends FunSpec with Matchers {
  it("finds the credit line for an image-specific contributor code") {
    transformer.getCredit(
      miroId = "B0011308",
      miroData = MiroTransformableData(
        creditLine = None,
        sourceCode = Some("FDN")
      )
    ) shouldBe Some("Ezra Feilden")
  }

  val transformer = new MiroCredits { }
}
