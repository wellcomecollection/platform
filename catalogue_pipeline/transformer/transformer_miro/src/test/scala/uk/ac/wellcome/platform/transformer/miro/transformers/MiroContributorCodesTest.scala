package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.transformer.miro.exceptions.ShouldNotTransformException

class MiroContributorCodesTest extends FunSpec with Matchers {
  it("looks up a contributor code in the general map") {
    transformer.lookupContributorCode(miroId = "B0000001", code = "CSP") shouldBe Some(
      "Wellcome Collection")
  }

  it(
    "looks up a contributor code in the per-record map if it's absent from the general map") {
    transformer.lookupContributorCode(miroId = "B0006507", code = "CSC") shouldBe Some(
      "Jenny Nichols, Wellcome Trust Centre for Stem Cell Research")
  }

  it("uses the uppercased version of a contributor code") {
    transformer.lookupContributorCode(miroId = "B0000001", code = "csp") shouldBe Some(
      "Wellcome Collection")
  }

  it("returns None if it cannot find a contributor code") {
    transformer.lookupContributorCode(miroId = "XXX", code = "XXX") shouldBe None
  }

  it("rejects some images from contributor code GUS") {
    val caught = intercept[ShouldNotTransformException] {
      transformer.lookupContributorCode(miroId = "B0009891", code = "GUS")
    }
    caught.getMessage shouldBe s"Image B0009891 from contributor GUS should not be sent to the pipeline"
  }

  it("allows images from contributor code GUS which aren't on the ban list") {
    transformer.lookupContributorCode(miroId = "B0009889", code = "GUS") shouldBe Some(
      "Karen Gustafson")
  }

  val transformer = new MiroContributorCodes {}
}
