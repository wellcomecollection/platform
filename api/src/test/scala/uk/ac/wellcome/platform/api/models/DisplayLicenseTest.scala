package uk.ac.wellcome.platform.api.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._

class DisplayLicenseTest extends FunSpec with Matchers {

  it("should read a License as a DisplayLicense correctly") {
    val licenseType = "CC-Test"
    val label = "A fictional license for testing"
    val url = "http://creativecommons.org/licenses/test/-1.0/"

    val internalLicense = License(
      licenseType = licenseType,
      label = label,
      url = url
    )
    val displayLicense = DisplayLicense(internalLicense)

    displayLicense.licenseType shouldBe licenseType
    displayLicense.label shouldBe label
    displayLicense.url shouldBe url
  }
}
