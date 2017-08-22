package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil

class WorkTest extends FunSpec with Matchers {

  val identifiedWorkJson: String =
    """
      |{
      |  "canonicalId": "canonicalId",
      |  "identifiers": [
      |    {
      |      "identifierScheme": "identifierScheme",
      |      "value": "value"
      |    }
      |  ],
      |  "title": "title",
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
      |    "license": {
      |      "licenseType": "license",
      |      "label": "label",
      |      "url": "http://www.example.com",
      |      "type": "License"
      |    },
      |    "type": "Location"
      |  },
      |  "items": [
      |    {
      |      "canonicalId": "canonicalId",
      |      "identifiers": [
      |        {
      |          "identifierScheme": "identifierScheme",
      |          "value": "value"
      |        }
      |      ],
      |      "locations": [
      |        {
      |          "locationType": "location",
      |          "license": {
      |            "licenseType": "license",
      |            "label": "label",
      |            "url": "http://www.example.com",
      |            "type": "License"
      |          },
      |          "type": "Location"
      |        }
      |      ],
      |      "type": "Item"
      |    }
      |  ],
      |  "type": "Work"
      |}
    """.stripMargin.replaceAll("\\s", "")

  val unidentifiedWorkJson: String =
    """
      |{
      |  "identifiers": [
      |    {
      |      "identifierScheme": "identifierScheme",
      |      "value": "value"
      |    }
      |  ],
      |  "title": "title",
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
      |    "license": {
      |      "licenseType": "license",
      |      "label": "label",
      |      "url": "http://www.example.com",
      |      "type": "License"
      |    },
      |    "type": "Location"
      |  },
      |  "items": [
      |    {
      |      "canonicalId": "canonicalId",
      |      "identifiers": [
      |        {
      |          "identifierScheme": "identifierScheme",
      |          "value": "value"
      |        }
      |      ],
      |      "locations": [
      |        {
      |          "locationType": "location",
      |          "license": {
      |            "licenseType": "license",
      |            "label": "label",
      |            "url": "http://www.example.com",
      |            "type": "License"
      |          },
      |          "type": "Location"
      |        }
      |      ],
      |      "type": "Item"
      |    }
      |  ],
      |  "type": "Work"
      |}
    """.stripMargin.replaceAll("\\s", "")

  val license = License(
    licenseType = "license",
    label = "label",
    url = "http://www.example.com"
  )

  val location = Location(
    locationType = "location",
    url = None,
    license = license
  )

  val identifier = SourceIdentifier(
    identifierScheme = "identifierScheme",
    value = "value"
  )

  val item = Item(
    canonicalId = Some("canonicalId"),
    identifiers = List(identifier),
    locations = List(location)
  )

  val unidentifiedWork = Work(
    identifiers = List(identifier),
    title = "title",
    description = Some("description"),
    lettering = Some("lettering"),
    createdDate = Some(Period("period")),
    subjects = List(Concept("subject")),
    creators = List(Agent("47")),
    genres = List(Concept("genre")),
    thumbnail = Some(location),
    items = List(item)
  )

  val identifiedWork = Work(
    canonicalId = Some("canonicalId"),
    identifiers = unidentifiedWork.identifiers,
    title = unidentifiedWork.title,
    description = unidentifiedWork.description,
    lettering = unidentifiedWork.lettering,
    createdDate = unidentifiedWork.createdDate,
    subjects = unidentifiedWork.subjects,
    creators = unidentifiedWork.creators,
    genres = unidentifiedWork.genres,
    thumbnail = unidentifiedWork.thumbnail,
    items = unidentifiedWork.items
  )

  it("should serialise an unidentified Work as JSON") {
    val result = JsonUtil.toJson(unidentifiedWork)

    result.isSuccess shouldBe true
    result.get shouldBe unidentifiedWorkJson
  }

  it("should deserialize a JSON string as a unidentified Work") {
    val result = JsonUtil.fromJson[Work](unidentifiedWorkJson)

    result.isSuccess shouldBe true
    result.get shouldBe unidentifiedWork
  }

  it("should serialise an identified Item as Work") {
    val result = JsonUtil.toJson(identifiedWork)

    print(result)

    result.isSuccess shouldBe true
    result.get shouldBe identifiedWorkJson
  }

  it("should deserialize a JSON string as a identified Item") {
    val result = JsonUtil.fromJson[Work](identifiedWorkJson)

    result.isSuccess shouldBe true
    result.get shouldBe identifiedWork
  }

  it("should throw an UnidentifiableException when trying to get the id of an unidentified Work") {
    an [UnidentifiableException] should be thrownBy unidentifiedWork.id
  }
}
