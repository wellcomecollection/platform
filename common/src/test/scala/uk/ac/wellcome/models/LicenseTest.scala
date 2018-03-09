package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.utils.JsonUtil

class LicenseTest extends FunSpec with Matchers {

  it("should serialise a License as JSON") {
    val result = JsonUtil.toJson[License](License_CCBY)
    result.isSuccess shouldBe true
    result.get shouldBe """{"licenseType":"CC-BY","label":"Attribution 4.0 International (CC BY 4.0)","url":"http://creativecommons.org/licenses/by/4.0/","ontologyType":"License"}"""
  }

  it("should deserialise a JSON string as a License") {
    val licenseType = License_CC0.licenseType
    val label = License_CC0.label
    val url = License_CC0.url

    val jsonString = s"""
      {
        "licenseType": "$licenseType",
        "label": "$label",
        "url": "$url",
        "ontologyType": "License"
      }"""
    val result = JsonUtil.fromJson[License](jsonString)
    result.isSuccess shouldBe true

    val license = result.get
    license.licenseType shouldBe licenseType
    license.label shouldBe label
    license.url shouldBe url
  }
}
