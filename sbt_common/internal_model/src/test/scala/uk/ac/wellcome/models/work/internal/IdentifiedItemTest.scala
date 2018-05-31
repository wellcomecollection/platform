package uk.ac.wellcome.models.work.internal

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil._

class IdentifiedItemTest extends FunSpec with Matchers with JsonTestUtil {

  val identifiedItemJson: String =
    s"""
      |{
      |  "canonicalId": "canonicalId",
      |  "sourceIdentifier": {
      |      "identifierType": {
      |        "id": "${IdentifierType("miro-image-number").id}",
      |        "label": "${IdentifierType("miro-image-number").label}",
      |        "ontologyType": "IdentifierType"
      |      },
      |      "ontologyType": "Item",
      |      "value": "value"
      |  },
      |  "identifiers": [
      |    {
      |      "identifierType": {
      |        "id": "${IdentifierType("miro-image-number").id}",
      |        "label": "${IdentifierType("miro-image-number").label}",
      |        "ontologyType": "IdentifierType"
      |      },
      |      "ontologyType": "Item",
      |      "value": "value"
      |    }
      |  ],
      |  "locations": [
      |    {
      |      "locationType": "location",
      |      "url" : "",
      |      "credit" : null,
      |      "license": {
      |        "id": "${License_CCBY.id}",
      |        "label": "${License_CCBY.label}",
      |        "url": "${License_CCBY.url}",
      |        "ontologyType": "License"
      |      },
      |      "type": "DigitalLocation",
      |      "ontologyType": "DigitalLocation"
      |    }
      |  ],
      |  "ontologyType": "Item"
      |}
    """.stripMargin

  val location = DigitalLocation(
    locationType = "location",
    url = "",
    license = License_CCBY
  )

  val identifier = SourceIdentifier(
    identifierType = IdentifierType("miro-image-number"),
    ontologyType = "Item",
    value = "value"
  )

  val identifiedItem = IdentifiedItem(
    canonicalId = "canonicalId",
    sourceIdentifier = identifier,
    identifiers = List(identifier),
    locations = List(location)
  )

  it("should serialise an identified Item as JSON") {
    val result = toJson(identifiedItem)

    result.isSuccess shouldBe true
    assertJsonStringsAreEqual(result.get, identifiedItemJson)
  }

  it("should deserialize a JSON string as a identified Item") {
    val result = fromJson[IdentifiedItem](identifiedItemJson)

    result.isSuccess shouldBe true
    result.get shouldBe identifiedItem
  }
}
