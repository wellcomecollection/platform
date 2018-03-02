package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil._

class UnidentifiedWorkTest extends FunSpec with Matchers with JsonTestUtil {

  private val license_CCBYJson =
    s"""{
            "licenseType": "${License_CCBY.licenseType}",
            "label": "${License_CCBY.label}",
            "url": "${License_CCBY.url}",
            "type": "License"
          }"""

  // TRIVIA: This is an extract from Marco Polo's diaries, in which he
  // mistakes a rhinoceros for a unicorn.
  val physicalDescription = """
    |Hair like that of a buffalo, feet like those of an elephant, and a horn in
    |the middle of the forehead, which is black and very thick.""".stripMargin

  // TRIVIA: on 3 July 1998, LNER 4468 "Mallard" set the world speed record
  // for steam locomotives, reaching 126 mph.
  val publicationDate = "3 July 1938"

  val unidentifiedWorkJson: String =
    s"""
      |{
      |  "title": "title",
      |  "sourceIdentifier": {
      |    "identifierScheme": "${IdentifierSchemes.miroImageNumber.toString}",
      |    "value": "value"
      |  },
      |  "version": 1,
      |  "identifiers": [
      |    {
      |      "identifierScheme": "${IdentifierSchemes.miroImageNumber.toString}",
      |      "value": "value"
      |    }
      |  ],
      |  "description": "description",
      |  "physicalDescription": "$physicalDescription",
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
      |  "publicationDate": {
      |    "label": "$publicationDate",
      |    "type": "Period"
      |  },
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

  val item = UnidentifiedItem(
    sourceIdentifier = identifier,
    identifiers = List(identifier),
    locations = List(location)
  )

  val publisher = Organisation(
    label = "MIT Press"
  )

  val publishers = List(publisher)
  val unidentifiedWork = UnidentifiedWork(
    title = Some("title"),
    sourceIdentifier = identifier,
    version = 1,
    identifiers = List(identifier),
    description = Some("description"),
    physicalDescription = Some(physicalDescription),
    lettering = Some("lettering"),
    createdDate = Some(Period("period")),
    subjects = List(Concept("subject")),
    creators = List(Agent("47")),
    genres = List(Concept("genre")),
    thumbnail = Some(location),
    items = List(item),
    publishers = publishers,
    publicationDate = Some(Period(publicationDate))
  )

  it("should serialise an unidentified Work as JSON") {
    val result = toJson(unidentifiedWork)

    result.isSuccess shouldBe true

    assertJsonStringsAreEqual(result.get, unidentifiedWorkJson)
  }

  it("should deserialize a JSON string as a unidentified Work") {
    val result = fromJson[UnidentifiedWork](unidentifiedWorkJson)

    result.isSuccess shouldBe true
    result.get shouldBe unidentifiedWork
  }

  it("should have an ontology type 'Work' when serialised to JSON") {
    val jsonString = toJson(unidentifiedWork).get

    jsonString.contains("""type":"Work"""") should be(true)
  }
}
