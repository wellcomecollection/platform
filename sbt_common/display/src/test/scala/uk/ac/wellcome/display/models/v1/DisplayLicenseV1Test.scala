package uk.ac.wellcome.display.models.v1

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.License_CCBY

class DisplayLicenseV1Test extends FunSpec with Matchers {

  it("should read a License as a DisplayLicenseV1 correctly") {
    val displayLicense = DisplayLicenseV1(License_CCBY)

    displayLicense.licenseType shouldBe License_CCBY.id
    displayLicense.label shouldBe License_CCBY.label
    displayLicense.url shouldBe License_CCBY.url
    displayLicense.ontologyType shouldBe "License"
  }
}
