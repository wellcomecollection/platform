package uk.ac.wellcome.platform.transformer.transformers.miro

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.License_CC0

class MiroLicensesTest extends FunSpec with Matchers {

  it("finds a recognised license") {
    transformer.chooseLicense(useRestrictions = "CC-0") shouldBe License_CC0
  }

  val transformer = new MiroLicenses {}
}
