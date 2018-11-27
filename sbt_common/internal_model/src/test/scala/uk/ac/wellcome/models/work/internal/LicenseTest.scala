package uk.ac.wellcome.models.work.internal

import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._

class LicenseTest extends FunSpec with Matchers {

  it("can serialise and then deserialise a CC-BY license as JSON") {
    assertRoundTripsLicenseCorrectly(License_CCBY)
  }

  def assertRoundTripsLicenseCorrectly(license: License): Assertion = {
    val result = toJson[License](license)
    result.isSuccess shouldBe true

    val jsonString = result.get
    val parsedLicense = fromJson[License](jsonString)
    parsedLicense.isSuccess shouldBe true

    parsedLicense.get shouldBe license
  }
}
