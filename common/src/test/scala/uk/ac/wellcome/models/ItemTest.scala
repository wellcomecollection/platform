package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil

class ItemTest extends FunSpec with Matchers {

  val identifiedItemJson: String =
    """
      |{
      |  "canonicalId": "canonicalId",
      |  "identifiers": [
      |    {
      |      "identifierScheme": "identifierScheme",
      |      "value": "value"
      |    }
      |  ],
      |  "locations": [
      |    {
      |      "locationType": "location",
      |      "license": {
      |        "licenseType": "license",
      |        "label": "label",
      |        "url": "http://www.example.com",
      |        "type": "License"
      |      },
      |      "type": "Location"
      |    }
      |  ],
      |  "type": "Item"
      |}
    """.stripMargin.replaceAll("\\s", "")

  val unidentifiedItemJson: String =
    """
      |{
      |  "identifiers": [
      |    {
      |      "identifierScheme": "identifierScheme",
      |      "value": "value"
      |    }
      |  ],
      |  "locations": [
      |    {
      |      "locationType": "location",
      |      "license": {
      |        "licenseType": "license",
      |        "label": "label",
      |        "url": "http://www.example.com",
      |        "type": "License"
      |      },
      |      "type": "Location"
      |    }
      |  ],
      |  "type": "Item"
      |}
    """.stripMargin.replaceAll("\\s", "")

  val location = Location(
    locationType = "location",
    url = None,
    license = License_CCBY
  )

  val identifier = SourceIdentifier(
    identifierScheme = "identifierScheme",
    value = "value"
  )

  val unidentifiedItem = Item(
    canonicalId = None,
    identifiers = List(identifier),
    locations = List(location)
  )

  val identifiedItem = Item(
    canonicalId = Some("canonicalId"),
    identifiers = List(identifier),
    locations = List(location)
  )

  it("should serialise an unidentified Item as JSON") {
    val result = JsonUtil.toJson(unidentifiedItem)

    result.isSuccess shouldBe true
    result.get shouldBe unidentifiedItemJson
  }

  it("should deserialize a JSON string as a unidentified Item") {
    val result = JsonUtil.fromJson[Item](unidentifiedItemJson)

    result.isSuccess shouldBe true
    result.get shouldBe unidentifiedItem
  }

  it("should serialise an identified Item as JSON") {
    val result = JsonUtil.toJson(identifiedItem)

    result.isSuccess shouldBe true
    result.get shouldBe identifiedItemJson
  }

  it("should deserialize a JSON string as a identified Item") {
    val result = JsonUtil.fromJson[Item](identifiedItemJson)

    result.isSuccess shouldBe true
    result.get shouldBe identifiedItem
  }

  it("should throw an UnidentifiableException when trying to get the id of an unidentified Item") {
    an [UnidentifiableException] should be thrownBy unidentifiedItem.id
  }
}
