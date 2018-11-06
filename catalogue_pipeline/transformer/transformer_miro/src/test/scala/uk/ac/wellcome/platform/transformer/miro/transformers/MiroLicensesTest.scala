package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.License_CC0
import uk.ac.wellcome.platform.transformer.exceptions.ShouldNotTransformException

class MiroLicensesTest extends FunSpec with Matchers {
  val miroId = "V00001"

  it("finds a recognised license") {
    transformer.chooseLicense(miroId, Some("CC-0")) shouldBe License_CC0
  }

  it("rejects restrictions 'Do not use'") {
    intercept[ShouldNotTransformException] {
      transformer.chooseLicense(miroId, Some("Do not use"))
    }
  }

  it("rejects restrictions 'Image withdrawn, see notes'") {
    intercept[ShouldNotTransformException] {
      transformer.chooseLicense(miroId, Some("Image withdrawn, see notes"))
    }
  }

  it("rejects the record if the usage restrictions are unspecified") {
    intercept[ShouldNotTransformException] {
      transformer.chooseLicense(miroId, None)
    }
  }

  val transformer = new MiroLicenses {}
}
