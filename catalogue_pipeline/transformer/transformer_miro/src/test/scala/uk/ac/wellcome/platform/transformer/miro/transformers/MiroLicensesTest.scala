package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{
  License,
  License_CC0,
  License_CopyrightNotCleared
}
import uk.ac.wellcome.platform.transformer.miro.exceptions.ShouldNotTransformException

class MiroLicensesTest extends FunSpec with Matchers {
  it("finds a recognised license") {
    chooseLicense(Some("CC-0")) shouldBe License_CC0
  }

  it("accepts an 'In Copyright' record") {
    chooseLicense(Some("In copyright")) shouldBe License_CopyrightNotCleared
  }

  it("rejects restrictions 'Do not use'") {
    intercept[ShouldNotTransformException] {
      chooseLicense(Some("Do not use"))
    }
  }

  it("rejects restrictions 'Image withdrawn, see notes'") {
    intercept[ShouldNotTransformException] {
      chooseLicense(Some("Image withdrawn, see notes"))
    }
  }

  it("rejects the record if the usage restrictions are unspecified") {
    intercept[ShouldNotTransformException] {
      chooseLicense(maybeUseRestrictions = None)
    }
  }

  private def chooseLicense(maybeUseRestrictions: Option[String]): License =
    transformer.chooseLicense(
      miroId = "A1234567",
      maybeUseRestrictions = maybeUseRestrictions)

  val transformer = new MiroLicenses {}
}
