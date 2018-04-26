package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.utils.JsonTestUtil

class IdentifiedWorkTest extends FunSpec with Matchers with JsonTestUtil {

  private val license_CCBYJson =
    s"""{
            "licenseType": "${License_CCBY.licenseType}",
            "label": "${License_CCBY.label}",
            "url": "${License_CCBY.url}",
            "ontologyType": "License"
          }"""

  // TRIVIA: On 18 April 1930, the BBC had a slow news day.  The bulletin
  // read "There is no news", followed by 15 minutes of piano music.
  val publicationDate = "18 April 1930"

  // TRIVIA: This isn't describing a book, but instead the allocation
  // of disk space inside Microsoft SQL Server.
  val extent = "A collection of eight physically contiguous pages"

  // TRIVIA: This is how Willem de Vlamingh, a Dutch scientist, described
  // seeing the quokka when exploring near Australia.
  val physicalDescription = "A kind of rat as big as a cat"

  // wow very language such doge much javascript
  // (In the future, everything will compile to JavaScript.)
  val language = Language(
    id = "dog",
    label = "Dogescript"
  )

  // TRIVIA: This is the distance travelled by the Opportunity Mars rover,
  // as of 10 January 2018.
  val dimensions = "45 km (28 mi)"

  val identifiedWorkJson: String =
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
      |    "id": "id",
      |    "label": "label",
      |    "ontologyType" : "WorkType"
      |  },
      |  "canonicalId": "canonicalId",
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
      |      "label": "subject",
      |      "ontologyType": "Subject",
      |      "concepts" : [
      |        {
      |          "label" : "concept",
      |          "ontologyType" : "Concept"
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
      |      "label": "genre",
      |      "ontologyType": "Concept"
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
      |      "canonicalId": "canonicalId",
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
      |  "publishers": [
      |    {
      |      "agent" : {
      |        "label" : "MIT Press",
      |        "type" : "Organisation"
      |      },
      |      "type" : "Unidentifiable"
      |    }
      |  ],
      |  "publicationDate": {
      |    "label": "$publicationDate",
      |    "ontologyType": "Period"
      |  },
      |  "placesOfPublication": [{
      |   "label": "Spain",
      |   "ontologyType": "Place"
      |  }],
      |   "language": {
      |     "id": "${language.id}",
      |     "label": "${language.label}",
      |     "ontologyType": "Language"
      |   },
      |   "dimensions": "$dimensions",
      |  "visible":true,
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

  val item = IdentifiedItem(
    canonicalId = "canonicalId",
    sourceIdentifier = itemIdentifier,
    identifiers = List(itemIdentifier),
    locations = List(location)
  )

  val publisher = Organisation(
    label = "MIT Press"
  )

  val workType = WorkType(
    id = "id",
    label = "label"
  )

  val publishers = List(Unidentifiable(publisher))

  val identifiedWork = IdentifiedWork(
    canonicalId = "canonicalId",
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
    subjects = List(Subject("subject", List(Concept("concept")))),
    contributors = List(Contributor(agent = Unidentifiable(Agent("47")))),
    genres = List(Concept("genre")),
    thumbnail = Some(location),
    items = List(item),
    publishers = publishers,
    publicationDate = Some(Period(publicationDate)),
    placesOfPublication = List(Place(label = "Spain")),
    language = Some(language),
    dimensions = Some(dimensions)
  )

  it("should serialise an identified Item as Work") {
    val result = toJson(identifiedWork)

    result.isSuccess shouldBe true
    assertJsonStringsAreEqual(result.get, identifiedWorkJson)
  }

  it("should deserialize a JSON string as a identified Item") {
    val result = fromJson[IdentifiedWork](identifiedWorkJson)

    result.isSuccess shouldBe true
    result.get shouldBe identifiedWork
  }

  it("should have an ontology type 'Work' when serialised to JSON") {
    val jsonString = toJson(identifiedWork).get

    jsonString.contains("""ontologyType":"Work"""") should be(true)
  }
}
