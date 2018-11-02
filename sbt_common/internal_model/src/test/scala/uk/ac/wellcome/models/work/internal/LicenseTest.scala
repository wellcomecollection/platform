package uk.ac.wellcome.models.work.internal

import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._

class LicenseTest extends FunSpec with Matchers {

  it("can serialise and deserialise a License as a JSON string") {
    assertCanSendToJsonAndBack(License_CCBY)
  }

  it("can serialise and deserialise a License without a URL") {
    assertCanSendToJsonAndBack(License_CopyrightNotCleared)
  }

  it("throws an error if you try to deserialise an unrecognised license") {
    intercept[IllegalArgumentException] {
      fromJson[License]("""{"id": "not-a-license"}""")
    }
  }

  private def assertCanSendToJsonAndBack(license: License): Assertion = {
    val result = toJson[License](license)
    result.isSuccess shouldBe true

    val jsonString = result.get
    val parsedResult = fromJson[License](jsonString)
    parsedResult.isSuccess shouldBe true

    parsedResult.get shouldBe license
  }
}
