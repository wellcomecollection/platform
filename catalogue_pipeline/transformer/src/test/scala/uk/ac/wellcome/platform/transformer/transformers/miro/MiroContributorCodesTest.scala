package uk.ac.wellcome.platform.transformer.transformers.miro

import org.scalatest.{FunSpec, Matchers}

class MiroContributorCodesTest extends FunSpec with Matchers {
  it("looks up a contributor code in the general map") {
    transformer.lookupContributorCode(miroId = "B0000001", code = "CSP") shouldBe Some("Wellcome Collection")
  }

  it("looks up a contributor code in the per-record map if it's absent from the general map") {
    transformer.lookupContributorCode(miroId = "B0006507", code = "CSC") shouldBe Some("Jenny Nichols")
  }

  it("returns None if it cannot find a contributor code") {
    transformer.lookupContributorCode(miroId = "XXX", code = "XXX") shouldBe None
  }

  val transformer = new MiroContributorCodes { }
}
