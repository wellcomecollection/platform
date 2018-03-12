package uk.ac.wellcome.models.display

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._

class DisplayLicenseTest extends FunSpec with Matchers {

  it("should read a License as a DisplayLicense correctly") {
    val displayLicense = DisplayLicense(License_CCBY)

    displayLicense.licenseType shouldBe License_CCBY.licenseType
    displayLicense.label shouldBe License_CCBY.label
    displayLicense.url shouldBe License_CCBY.url
    displayLicense.ontologyType shouldBe "License"
  }
}
