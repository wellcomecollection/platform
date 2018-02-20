package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.utils.JsonTestUtil

class WorkTest extends FunSpec with Matchers with JsonTestUtil {

  private val license_CCBYJson =
    s"""{
            "licenseType": "${License_CCBY.licenseType}",
            "label": "${License_CCBY.label}",
            "url": "${License_CCBY.url}",
            "type": "License"
          }"""

  val identifiedWorkJson: String =
    s"""
      |{
      |  "title": "title",
      |  "sourceIdentifier": {
      |    "identifierScheme": "${IdentifierSchemes.miroImageNumber.toString}",
      |    "value": "value"
      |  },
      |  "identifiers": [
      |    {
      |      "identifierScheme": "${IdentifierSchemes.miroImageNumber.toString}",
      |      "value": "value"
      |    }
      |  ],
      |  "canonicalId": "canonicalId",
      |  "description": "description",
      |  "lettering": "lettering",
      |  "createdDate": {
      |    "label": "period",
      |    "type": "Period"
      |  },
      |  "subjects": [
      |    {
      |      "label": "subject",
      |      "type": "Concept"
      |    }
      |  ],
      |  "creators": [
      |    {
      |      "label": "47",
      |      "type": "Agent"
      |    }
      |  ],
      |  "genres": [
      |    {
      |      "label": "genre",
      |      "type": "Concept"
      |    }
      |  ],
      |  "thumbnail": {
      |    "locationType": "location",
      |    "url" : "",
      |    "credit" : null,
      |    "license": $license_CCBYJson,
      |    "type": "DigitalLocation"
      |  },
      |  "items": [
      |    {
      |      "canonicalId": "canonicalId",
      |      "sourceIdentifier": {
      |        "identifierScheme": "${IdentifierSchemes.miroImageNumber.toString}",
      |        "value": "value"
      |      },
      |      "identifiers": [
      |        {
      |          "identifierScheme": "${IdentifierSchemes.miroImageNumber.toString}",
      |          "value": "value"
      |        }
      |      ],
      |      "locations": [
      |        {
      |          "locationType": "location",
      |          "url" : "",
      |          "credit" : null,
      |          "license": $license_CCBYJson,
      |          "type": "DigitalLocation"
      |        }
      |      ],
      |      "visible":true,
      |      "type": "Item"
      |    }
      |  ],
      |  "publishers": [
      |    {
      |      "label": "MIT Press",
      |      "type": "Organisation"
      |    }
      |  ],
      |  "visible":true,
      |  "type": "Work"
      |}
    """.stripMargin

  val unidentifiedWorkJson: String =
    s"""
      |{
      |  "title": "title",
      |  "sourceIdentifier": {
      |    "identifierScheme": "${IdentifierSchemes.miroImageNumber.toString}",
      |    "value": "value"
      |  },
      |  "canonicalId" : null,
      |  "identifiers": [
      |    {
      |      "identifierScheme": "${IdentifierSchemes.miroImageNumber.toString}",
      |      "value": "value"
      |    }
      |  ],
      |  "description": "description",
      |  "lettering": "lettering",
      |  "createdDate": {
      |    "label": "period",
      |    "type": "Period"
      |  },
      |  "subjects": [
      |    {
      |      "label": "subject",
      |      "type": "Concept"
      |    }
      |  ],
      |  "creators": [
      |    {
      |      "label": "47",
      |      "type": "Agent"
      |    }
      |  ],
      |  "genres": [
      |    {
      |      "label": "genre",
      |      "type": "Concept"
      |    }
      |  ],
      |  "thumbnail": {
      |    "locationType": "location",
      |    "url" : "",
      |    "credit" : null,
      |    "license": $license_CCBYJson,
      |    "type": "DigitalLocation"
      |  },
      |  "items": [
      |    {
      |      "canonicalId": "canonicalId",
      |      "sourceIdentifier": {
      |        "identifierScheme": "${IdentifierSchemes.miroImageNumber.toString}",
      |        "value": "value"
      |      },
      |      "identifiers": [
      |        {
      |          "identifierScheme": "${IdentifierSchemes.miroImageNumber.toString}",
      |          "value": "value"
      |        }
      |      ],
      |      "locations": [
      |        {
      |          "locationType": "location",
      |          "url" : "",
      |          "credit" : null,
      |          "license": $license_CCBYJson,
      |          "type": "DigitalLocation"
      |        }
      |      ],
      |      "visible":true,
      |      "type": "Item"
      |    }
      |  ],
      |  "publishers": [
      |    {
      |      "label": "MIT Press",
      |      "type": "Organisation"
      |    }
      |  ],
      |  "visible":true,
      |  "type": "Work"
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

  val item = Item(
    canonicalId = Some("canonicalId"),
    sourceIdentifier = identifier,
    identifiers = List(identifier),
    locations = List(location)
  )

  val publisher = Organisation(
    label = "MIT Press"
  )

  val publishers = List(publisher)
  val unidentifiedWork = Work(
    title = Some("title"),
    sourceIdentifier = identifier,
    version = 1,
    identifiers = List(identifier),
    description = Some("description"),
    lettering = Some("lettering"),
    createdDate = Some(Period("period")),
    subjects = List(Concept("subject")),
    creators = List(Agent("47")),
    genres = List(Concept("genre")),
    thumbnail = Some(location),
    items = List(item),
    publishers = publishers
  )

  val identifiedWork = Work(
    title = unidentifiedWork.title,
    sourceIdentifier = identifier,
    version = 1,
    identifiers = unidentifiedWork.identifiers,
    canonicalId = Some("canonicalId"),
    description = unidentifiedWork.description,
    lettering = unidentifiedWork.lettering,
    createdDate = unidentifiedWork.createdDate,
    subjects = unidentifiedWork.subjects,
    creators = unidentifiedWork.creators,
    genres = unidentifiedWork.genres,
    thumbnail = unidentifiedWork.thumbnail,
    items = unidentifiedWork.items,
    publishers = publishers
  )

  it("should serialise an unidentified Work as JSON") {
    val result = toJson(unidentifiedWork)

    result.isSuccess shouldBe true

    assertJsonStringsAreEqual(result.get, unidentifiedWorkJson)
  }

  it("should deserialize a JSON string as a unidentified Work") {
    val result = fromJson[Work](unidentifiedWorkJson)

    result.isSuccess shouldBe true
    result.get shouldBe unidentifiedWork
  }

  it("should serialise an identified Item as Work") {
    val result = toJson(identifiedWork)

    result.isSuccess shouldBe true
    assertJsonStringsAreEqual(result.get, identifiedWorkJson)
  }

  it("should deserialize a JSON string as a identified Item") {
    val result = fromJson[Work](identifiedWorkJson)

    result.isSuccess shouldBe true
    result.get shouldBe identifiedWork
  }

  it(
    "should throw an UnidentifiableException when trying to get the id of an unidentified Work") {
    an[UnidentifiableException] should be thrownBy unidentifiedWork.id
  }

  it("should have an ontology type 'Work' when serialised to JSON") {
    val work = Work(title = Some("A book about a blue whale"),
                    sourceIdentifier = identifier,
                    version = 1,
                    identifiers = List(identifier))
    val jsonString = toJson(work).get

    jsonString.contains("""type":"Work"""") should be(true)
  }
}
