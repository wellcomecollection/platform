package uk.ac.wellcome.platform.transformer.transformers.miro

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.License_CC0

class MiroLicensesTest extends FunSpec with Matchers {

  it("finds a recognised license") {
    transformer.chooseLicense(Some("CC-0")) shouldBe License_CC0
  }

  it("rejects restrictions 'Do not use'") {
    intercept[ShouldNotTransformException] {
      transformer.chooseLicense(Some("Do not use"))
    }
  }

  it("rejects restrictions 'Image withdrawn, see notes'") {
    intercept[ShouldNotTransformException] {
      transformer.chooseLicense(Some("Image withdrawn, see notes"))
    }
  }

  it("rejects the record if the usage restrictions are unspecified") {
    intercept[ShouldNotTransformException] {
      transformer.chooseLicense(None)
    }
  }

  val transformer = new MiroLicenses {}
}
