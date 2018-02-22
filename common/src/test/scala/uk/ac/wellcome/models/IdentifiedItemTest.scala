package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil

class IdentifiedItemTest extends FunSpec with Matchers with JsonTestUtil {

  val identifiedItemJson: String =
    s"""
      |{
      |  "canonicalId": "canonicalId",
      |  "sourceIdentifier": {
      |      "identifierScheme": "${IdentifierSchemes.miroImageNumber.toString}",
      |      "value": "value"
      |  },
      |  "identifiers": [
      |    {
      |      "identifierScheme": "${IdentifierSchemes.miroImageNumber.toString}",
      |      "value": "value"
      |    }
      |  ],
      |  "locations": [
      |    {
      |      "locationType": "location",
      |      "url" : "",
      |      "credit" : null,
      |      "license": {
      |        "licenseType": "${License_CCBY.licenseType}",
      |        "label": "${License_CCBY.label}",
      |        "url": "${License_CCBY.url}",
      |        "type": "License"
      |      },
      |      "type": "DigitalLocation"
      |    }
      |  ],
      |  "visible":true,
      |  "type": "Item"
      |}
    """.stripMargin

  val location = DigitalLocation(
    locationType = "location",
    url = "",
    license = License_CCBY
  )

  val identifier = SourceIdentifier(
    identifierScheme = IdentifierSchemes.miroImageNumber,
    value = "value"
  )

  val identifiedItem = IdentifiedItem(
    canonicalId = "canonicalId",
    sourceIdentifier = identifier,
    identifiers = List(identifier),
    locations = List(location)
  )

  it("should serialise an identified Item as JSON") {
    val result = JsonUtil.toJson(identifiedItem)

    result.isSuccess shouldBe true
    assertJsonStringsAreEqual(result.get, identifiedItemJson)
  }

  it("should deserialize a JSON string as a identified Item") {
    val result = JsonUtil.fromJson[IdentifiedItem](identifiedItemJson)

    result.isSuccess shouldBe true
    result.get shouldBe identifiedItem
  }
}
