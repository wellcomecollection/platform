package uk.ac.wellcome.models.work.internal

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.test.util.WorksUtil
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil._

class UnidentifiedWorkTest
    extends FunSpec
    with Matchers
    with JsonTestUtil
    with WorksUtil {

  private val license_CCBYJson =
    s"""{
            "licenseType": "${License_CCBY.licenseType}",
            "label": "${License_CCBY.label}",
            "url": "${License_CCBY.url}",
            "ontologyType": "License"
          }"""

  // TRIVIA: This is an extract from Marco Polo's diaries, in which he
  // mistakes a rhinoceros for a unicorn.
  val physicalDescription =
    "Hair like that of a buffalo, feet like those of an elephant"

  // TRIVIA: This is based on Harry Potter, when we first meet Dobby.
  val extent = "Both socks pulled up to their highest extent"

  // TRIVIA: on 3 July 1998, LNER 4468 "Mallard" set the world speed record
  // for steam locomotives, reaching 126 mph.
  val publicationDate = "3 July 1938"

  // Reskitkish is a fictional language from the "Wayfarers" series of novels,
  // and requires large lungs to speak effectively.
  val language = Language(
    id = "res",
    label = "Reskitkish"
  )

  // According to Google Maps, the distance between 183 and 215 Euston Road.
  val dimensions = "308 ft"

  val unidentifiedWorkJson: String =
    s"""
      |{
      |  "title": "title",
      |  "sourceIdentifier": {
      |    "identifierScheme": "${IdentifierSchemes.miroImageNumber.toString}",
      |    "ontologyType": "Work",
      |    "value": "value"
      |  },
      |  "version": 1,
      |  "identifiers": [
      |    {
      |      "identifierScheme": "${IdentifierSchemes.miroImageNumber.toString}",
      |      "ontologyType": "Work",
      |      "value": "value"
      |    }
      |  ],
      |  "workType": {
      |    "id": "${workType.id}",
      |    "label": "${workType.label}",
      |    "ontologyType" : "${workType.ontologyType}"
      |  },
      |  "description": "description",
      |  "physicalDescription": "$physicalDescription",
      |  "extent": "$extent",
      |  "lettering": "lettering",
      |  "createdDate": {
      |    "label": "period",
      |    "ontologyType": "Period"
      |  },
      |  "subjects": [
      |    {
      |      "label": "${subject.label}",
      |      "ontologyType": "${subject.ontologyType}",
      |      "concepts" : [
      |        {
      |          "label" : "${subject.concepts(0).label}",
      |          "ontologyType" : "${subject.concepts(0).ontologyType}",
      |          "type" : "${subject.concepts(0).ontologyType}"
      |        },
      |        {
      |          "label" : "${subject.concepts(1).label}",
      |          "ontologyType" : "${subject.concepts(1).ontologyType}",
      |          "type" : "${subject.concepts(1).ontologyType}"
      |        },
      |        {
      |          "label" : "${subject.concepts(2).label}",
      |          "ontologyType" : "${subject.concepts(2).ontologyType}",
      |          "type" : "${subject.concepts(2).ontologyType}"
      |        }
      |      ]
      |    }
      |  ],
      |  "contributors": [
      |    {
      |      "agent": {
      |        "agent": {
      |          "label": "47",
      |          "type" : "Agent"
      |        },
      |        "type": "Unidentifiable"
      |      },
      |      "roles": [],
      |      "ontologyType": "Contributor"
      |    }
      |  ],
      |  "genres": [
      |    {
      |      "label": "${genre.label}",
      |      "ontologyType": "${genre.ontologyType}",
      |      "concepts" : [
      |        {
      |          "label" : "${genre.concepts(0).label}",
      |          "ontologyType" : "${genre.concepts(0).ontologyType}",
      |          "type" : "${genre.concepts(0).ontologyType}"
      |        },
      |        {
      |          "label" : "${genre.concepts(1).label}",
      |          "ontologyType" : "${genre.concepts(1).ontologyType}",
      |          "type" : "${genre.concepts(1).ontologyType}"
      |        },
      |        {
      |          "label" : "${genre.concepts(2).label}",
      |          "ontologyType" : "${genre.concepts(2).ontologyType}",
      |          "type" : "${genre.concepts(2).ontologyType}"
      |        }
      |      ]
      |    }
      |  ],
      |  "thumbnail": {
      |    "locationType": "location",
      |    "url" : "",
      |    "credit" : null,
      |    "license": $license_CCBYJson,
      |    "type": "DigitalLocation",
      |    "ontologyType": "DigitalLocation"
      |  },
      |  "items": [
      |    {
      |      "sourceIdentifier": {
      |        "identifierScheme": "${IdentifierSchemes.miroImageNumber.toString}",
      |        "ontologyType": "Item",
      |        "value": "value"
      |      },
      |      "identifiers": [
      |        {
      |          "identifierScheme": "${IdentifierSchemes.miroImageNumber.toString}",
      |          "ontologyType": "Item",
      |          "value": "value"
      |        }
      |      ],
      |      "locations": [
      |        {
      |          "locationType": "location",
      |          "url" : "",
      |          "credit" : null,
      |          "license": $license_CCBYJson,
      |          "type": "DigitalLocation",
      |          "ontologyType": "DigitalLocation"
      |        }
      |      ],
      |      "ontologyType": "Item"
      |    }
      |  ],
      |  "publishers" : [
      |    {
      |      "agent" : {
      |        "label" : "MIT Press",
      |        "type" : "Organisation"
      |      },
      |      "type" : "Unidentifiable"
      |    }
      |  ],
      |  "visible":true,
      |  "publicationDate": {
      |    "label": "$publicationDate",
      |    "ontologyType": "Period"
      |  },
      |  "placesOfPublication": [
      |   {
      |     "label": "Madrid",
      |     "ontologyType": "Place"
      |   }
      |  ],
      |   "language": {
      |     "id": "${language.id}",
      |     "label": "${language.label}",
      |     "ontologyType": "Language"
      |   },
      |   "dimensions": "$dimensions",
      |  "ontologyType": "Work"
      |}
    """.stripMargin

  val location = DigitalLocation(
    locationType = "location",
    url = "",
    license = License_CCBY
  )

  val workIdentifier = SourceIdentifier(
    identifierScheme = IdentifierSchemes.miroImageNumber,
    ontologyType = "Work",
    value = "value"
  )

  val itemIdentifier = SourceIdentifier(
    identifierScheme = IdentifierSchemes.miroImageNumber,
    ontologyType = "Item",
    value = "value"
  )

  val item = UnidentifiedItem(
    sourceIdentifier = itemIdentifier,
    identifiers = List(itemIdentifier),
    locations = List(location)
  )

  val publisher = Organisation(
    label = "MIT Press"
  )

  val publishers = List(Unidentifiable(publisher))

  val unidentifiedWork = UnidentifiedWork(
    title = Some("title"),
    sourceIdentifier = workIdentifier,
    version = 1,
    identifiers = List(workIdentifier),
    workType = Some(workType),
    description = Some("description"),
    physicalDescription = Some(physicalDescription),
    extent = Some(extent),
    lettering = Some("lettering"),
    createdDate = Some(Period("period")),
    subjects = List(subject),
    contributors = List(
      Contributor(
        agent = Unidentifiable(Agent("47"))
      )),
    genres = List(genre),
    thumbnail = Some(location),
    items = List(item),
    publishers = publishers,
    publicationDate = Some(Period(publicationDate)),
    placesOfPublication = List(Place("Madrid")),
    language = Some(language),
    dimensions = Some(dimensions)
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

    jsonString.contains("""ontologyType":"Work"""") should be(true)
  }
}
