package uk.ac.wellcome.models.work.internal

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._

class LicenseTest extends FunSpec with Matchers {

  it("serialises a License as JSON") {
    val result = toJson[License](License_CCBY)
    result.isSuccess shouldBe true
    result.get shouldBe """{"id":"cc-by","label":"Attribution 4.0 International (CC BY 4.0)","url":"http://creativecommons.org/licenses/by/4.0/","ontologyType":"License"}"""
  }

  it("deserialises a JSON string as a License") {
    val id = License_CC0.id
    val label = License_CC0.label
    val url = License_CC0.url

    val jsonString = s"""
      {
        "id": "$id",
        "label": "$label",
        "url": "$url",
        "ontologyType": "License"
      }"""
    val result = fromJson[License](jsonString)
    result.isSuccess shouldBe true

    val license = result.get
    license.id shouldBe id
    license.label shouldBe label
    license.url shouldBe Some(url)
  }
}
