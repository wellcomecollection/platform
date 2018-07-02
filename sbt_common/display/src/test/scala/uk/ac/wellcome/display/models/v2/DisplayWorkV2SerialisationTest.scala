package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.WorksIncludes
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.WorksUtil

class DisplayWorkV2SerialisationTest
    extends FunSpec
    with DisplayV2SerialisationTestBase
    with JsonMapperTestUtil
    with WorksUtil {

  it("serialises a DisplayWorkV2 correctly") {

    val work = workWith(
      canonicalId = canonicalId,
      title = title,
      description = description,
      lettering = lettering,
      createdDate = period,
      creator = agent,
      items = createItems(count = 2))

    val actualJsonString = objectMapper.writeValueAsString(DisplayWorkV2(work))

    val expectedJsonString = s"""
       |{
       | "type": "Work",
       | "id": "$canonicalId",
       | "title": "$title",
       | "description": "$description",
       | "workType": {
       |       "id": "${workType.id}",
       |       "label": "${workType.label}",
       |       "type": "WorkType"
       | },
       | "lettering": "$lettering",
       | "createdDate": ${period(work.createdDate.get)},
       | "contributors": [
       |   {
       |     "agent": {
       |       "label": "a person",
       |       "type": "Agent"
       |     },
       |     "roles" : [ ],
       |     "type" : "Contributor"
       |   }
       | ],
       | "subjects": [ ],
       | "genres": [ ],
       | "production": [ ]
       |}
          """.stripMargin

    assertJsonStringsAreEqual(actualJsonString, expectedJsonString)
  }

  it("renders an item if the items include is present") {
    val work = workWith(
      canonicalId = "b4heraz7",
      title = "Inside an irate igloo",
      items = createItems(count = 1)
    )

    val actualJson = objectMapper.writeValueAsString(
      DisplayWorkV2(work, WorksIncludes(items = true)))
    val expectedJson = s"""
                          |{
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "title": "${work.title}",
                          | "contributors": [ ],
                          | "items": [ ${items(work.items)} ],
                          | "subjects": [ ],
                          | "genres": [ ],
                          | "production": [ ]
                          |}
          """.stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("includes 'items' if the items include is present, even with no items") {
    val work = workWith(
      canonicalId = "dgdb712",
      title = "Without windows or wind or washing-up liquid",
      items = List()
    )
    val actualJson = objectMapper.writeValueAsString(
      DisplayWorkV2(work, WorksIncludes(items = true)))
    val expectedJson = s"""
                          |{
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "title": "${work.title}",
                          | "contributors": [ ],
                          | "items": [ ],
                          | "subjects": [ ],
                          | "genres": [ ],
                          | "production": [ ]
                          |}
          """.stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("includes credit information in DisplayWorkV2 serialisation") {
    val location = DigitalLocation(
      locationType = LocationType("thumbnail-image"),
      url = "",
      credit = Some("Wellcome Collection"),
      license = License_CCBY
    )
    val item = createItem(locations = List(location))
    val workWithCopyright = IdentifiedWork(
      title = "A scarf on a squirrel",
      sourceIdentifier = sourceIdentifier,
      version = 1,
      canonicalId = "yxh928a",
      items = List(item))

    val actualJson = objectMapper.writeValueAsString(
      DisplayWorkV2(workWithCopyright, WorksIncludes(items = true)))
    val expectedJson = s"""{
                          |     "type": "Work",
                          |     "id": "${workWithCopyright.canonicalId}",
                          |     "title": "${workWithCopyright.title}",
                          |     "contributors": [ ],
                          |     "subjects": [ ],
                          |     "genres": [ ],
                          |     "production": [ ],
                          |     "items": [
                          |       {
                          |         "id": "${item.canonicalId}",
                          |         "type": "${item.agent.ontologyType}",
                          |         "locations": [
                          |           {
                          |             "type": "${location.ontologyType}",
                          |             "url": "",
                          |             "locationType": ${locationType(
                            location.locationType)},
                          |             "license": ${license(location.license)},
                          |             "credit": "${location.credit.get}"
                          |           }
                          |         ]
                          |       }
                          |     ]
                          |   }""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("includes subject information in DisplayWorkV2 serialisation") {
    val workWithSubjects = IdentifiedWork(
      title = "A seal selling seaweed sandwiches in Scotland",
      sourceIdentifier = sourceIdentifier,
      version = 1,
      canonicalId = "test_subject1",
      subjects = List(
        Subject("label", List(Unidentifiable(Concept("fish")))),
        Subject("label", List(Unidentifiable(Concept("gardening")))))
    )
    val actualJson =
      objectMapper.writeValueAsString(DisplayWorkV2(workWithSubjects))
    val expectedJson = s"""{
                          |     "type": "Work",
                          |     "id": "${workWithSubjects.canonicalId}",
                          |     "title": "${workWithSubjects.title}",
                          |     "contributors": [],
                          |     "subjects": [ ${subjects(
                            workWithSubjects.subjects)} ],
                          |     "genres": [ ],
                          |     "production": [ ]
                          |   }""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("includes genre information in DisplayWorkV2 serialisation") {
    val workWithSubjects = IdentifiedWork(
      title = "A guppy in a greenhouse",
      sourceIdentifier = sourceIdentifier,
      version = 1,
      canonicalId = "test_subject1",
      genres = List(
        Genre(
          label = "genre",
          concepts = List(
            Unidentifiable(Concept("woodwork")),
            Unidentifiable(Concept("etching"))
          )
        ))
    )
    val actualJson =
      objectMapper.writeValueAsString(DisplayWorkV2(workWithSubjects))
    val expectedJson = s"""
                          |{
                          |     "type": "Work",
                          |     "id": "${workWithSubjects.canonicalId}",
                          |     "title": "${workWithSubjects.title}",
                          |     "contributors": [],
                          |     "subjects": [ ],
                          |     "genres": [ ${genres(workWithSubjects.genres)} ],
                          |     "production": [ ]
                          |   }""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("includes a list of identifiers on DisplayWorkV2") {
    val otherIdentifier = SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      ontologyType = "Work",
      value = "Test1234"
    )
    val work = workWith(
      canonicalId = "1234",
      title = "An insect huddled in an igloo",
      otherIdentifiers = List(otherIdentifier)
    )
    val actualJson = objectMapper.writeValueAsString(
      DisplayWorkV2(work, WorksIncludes(identifiers = true)))
    val expectedJson = s"""
                          |{
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "title": "${work.title}",
                          | "contributors": [ ],
                          | "identifiers": [ ${identifier(sourceIdentifier)}, ${identifier(
                            otherIdentifier)} ],
                          | "subjects": [ ],
                          | "genres": [ ],
                          | "production": [ ]
                          |}
          """.stripMargin
    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("always includes 'identifiers' with the identifiers include") {
    val work = workWith(
      canonicalId = "a87na87",
      title = "Idling inkwells of indigo images",
      otherIdentifiers = List()
    )
    val actualJson = objectMapper.writeValueAsString(
      DisplayWorkV2(work, WorksIncludes(identifiers = true)))
    val expectedJson = s"""
                          |{
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "title": "${work.title}",
                          | "contributors": [ ],
                          | "identifiers": [ ${identifier(sourceIdentifier)} ],
                          | "subjects": [ ],
                          | "genres": [ ],
                          | "production": [ ]
                          |}
          """.stripMargin
    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it(
    "includes the thumbnail field if available and we use the thumbnail include") {
    val work = identifiedWorkWith(
      canonicalId = "1234",
      title = "A thorn in the thumb tells a traumatic tale",
      thumbnail = DigitalLocation(
        locationType = LocationType("thumbnail-image"),
        url = "https://iiif.example.org/1234/default.jpg",
        license = License_CCBY
      )
    )
    val actualJson = objectMapper.writeValueAsString(
      DisplayWorkV2(work, WorksIncludes(thumbnail = true)))
    val expectedJson = s"""
                          |   {
                          |     "type": "Work",
                          |     "id": "${work.canonicalId}",
                          |     "title": "${work.title}",
                          |     "contributors": [ ],
                          |     "subjects": [ ],
                          |     "genres": [ ],
                          |     "production": [ ],
                          |     "thumbnail": ${location(work.thumbnail.get)}
                          |   }
          """.stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }
}
