package uk.ac.wellcome.display.models.v1

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.License_CCBY

class DisplayLicenseV1Test extends FunSpec with Matchers {

  it("reads a License as a DisplayLicenseV1") {
    val displayLicense = DisplayLicenseV1(License_CCBY)

    // These outputs are deliberately hard-coded -- because the V1 API
    // is public, we shouldn't be changing the output even when the
    // internal model changes.
    displayLicense.licenseType shouldBe "CC-BY"
    displayLicense.label shouldBe "Attribution 4.0 International (CC BY 4.0)"
    displayLicense.url shouldBe Some(
      "http://creativecommons.org/licenses/by/4.0/")
    displayLicense.ontologyType shouldBe "License"
  }
}
