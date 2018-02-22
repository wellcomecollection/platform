package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.utils.JsonUtil._

class UnidentifiedItemTest extends FunSpec with Matchers with JsonTestUtil {

  val unidentifiedItemJson: String =
    s"""
      |{
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

  val unidentifiedItem = UnidentifiedItem(
    sourceIdentifier = identifier,
    identifiers = List(identifier),
    locations = List(location)
  )

  it("should serialise an unidentified Item as JSON") {
    val result = JsonUtil.toJson(unidentifiedItem)

    result.isSuccess shouldBe true
    assertJsonStringsAreEqual(result.get, unidentifiedItemJson)
  }

  it("should deserialize a JSON string as a unidentified Item") {
    val result = JsonUtil.fromJson[Item](unidentifiedItemJson)

    result.isSuccess shouldBe true
    result.get shouldBe unidentifiedItem
  }
}
