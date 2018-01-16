package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.circe.jsonUtil
import uk.ac.wellcome.circe.jsonUtil._
import uk.ac.wellcome.test.utils.JsonTestUtil

class ItemTest extends FunSpec with Matchers with JsonTestUtil {

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
      |      "url" : null,
      |      "credit" : null,
      |      "license": {
      |        "licenseType": "${License_CCBY.licenseType}",
      |        "label": "${License_CCBY.label}",
      |        "url": "${License_CCBY.url}"
      |      }
      |    }
      |  ],
      |  "visible":true,
      |  "type": "Item"
      |}
    """.stripMargin

  val unidentifiedItemJson: String =
    s"""
      |{
      |  "canonicalId" : null,
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
      |      "url" : null,
      |      "credit" : null,
      |      "license": {
      |        "licenseType": "${License_CCBY.licenseType}",
      |        "label": "${License_CCBY.label}",
      |        "url": "${License_CCBY.url}"
      |      }
      |    }
      |  ],
      |  "visible":true,
      |  "type": "Item"
      |}
    """.stripMargin

  val location = Location(
    locationType = "location",
    url = None,
    license = License_CCBY
  )

  val identifier = SourceIdentifier(
    identifierScheme = IdentifierSchemes.miroImageNumber,
    value = "value"
  )

  val unidentifiedItem = Item(
    canonicalId = None,
    sourceIdentifier = identifier,
    identifiers = List(identifier),
    locations = List(location)
  )

  val identifiedItem = Item(
    canonicalId = Some("canonicalId"),
    sourceIdentifier = identifier,
    identifiers = List(identifier),
    locations = List(location)
  )

  it("should serialise an unidentified Item as JSON") {
    val result = jsonUtil.toJson(unidentifiedItem)

    result.isSuccess shouldBe true
    assertJsonStringsAreEqual(result.get, unidentifiedItemJson)
  }

  it("should deserialize a JSON string as a unidentified Item") {
    val result = jsonUtil.fromJson[Item](unidentifiedItemJson)

    result.isSuccess shouldBe true
    result.get shouldBe unidentifiedItem
  }

  it("should serialise an identified Item as JSON") {
    val result = jsonUtil.toJson(identifiedItem)

    result.isSuccess shouldBe true
    assertJsonStringsAreEqual(result.get, identifiedItemJson)
  }

  it("should deserialize a JSON string as a identified Item") {
    val result = jsonUtil.fromJson[Item](identifiedItemJson)

    result.isSuccess shouldBe true
    result.get shouldBe identifiedItem
  }

  it(
    "should throw an UnidentifiableException when trying to get the id of an unidentified Item") {
    an[UnidentifiableException] should be thrownBy unidentifiedItem.id
  }
}
