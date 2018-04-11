package uk.ac.wellcome.display.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Guice
import org.scalatest.FunSpec
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.models._
import uk.ac.wellcome.test.utils.JsonTestUtil

class DisplayWorkSerialisationTest
    extends FunSpec
    with DisplaySerialisationTestBase
    with JsonTestUtil
    with WorksUtil {
  val injector = Guice.createInjector(DisplayJacksonModule)

  val objectMapper = injector.getInstance(classOf[ObjectMapper])

  it("should serialise a DisplayWork correctly") {

    val work = workWith(
      canonicalId = canonicalId,
      title = title,
      description = description,
      lettering = lettering,
      createdDate = period,
      creator = agent,
      items = List(defaultItem),
      visible = true)

    val actualJsonString = objectMapper.writeValueAsString(DisplayWork(work))

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
       | "creators": [ ${identifiedOrUnidentifiable(
                                  work.creators(0),
                                  abstractAgent)} ],
       | "subjects": [ ],
       | "genres": [ ],
       | "publishers": [ ],
       | "placesOfPublication": [ ]
       |}
          """.stripMargin

    assertJsonStringsAreEqual(actualJsonString, expectedJsonString)
  }

  it("renders an item if the items include is present") {
    val work = workWith(
      canonicalId = "b4heraz7",
      title = "Inside an irate igloo",
      items = List(
        itemWith(
          canonicalId = "c3a599u5",
          identifier = defaultItemSourceIdentifier,
          location = defaultLocation
        )
      )
    )

    val actualJson = objectMapper.writeValueAsString(
      DisplayWork(work, WorksIncludes(items = true)))
    val expectedJson = s"""
                          |{
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "title": "${work.title.get}",
                          | "creators": [ ],
                          | "items": [ ${items(work.items)} ],
                          | "subjects": [ ],
                          | "genres": [ ],
                          | "publishers": [ ],
                          | "placesOfPublication": [ ]
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
      DisplayWork(work, WorksIncludes(items = true)))
    val expectedJson = s"""
                          |{
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "title": "${work.title.get}",
                          | "creators": [ ],
                          | "items": [ ],
                          | "subjects": [ ],
                          | "genres": [ ],
                          | "publishers": [ ],
                          | "placesOfPublication": [ ]
                          |}
          """.stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("includes credit information in DisplayWork serialisation") {
    val location = DigitalLocation(
      locationType = "thumbnail-image",
      url = "",
      credit = Some("Wellcome Collection"),
      license = License_CCBY
    )
    val item = IdentifiedItem(
      canonicalId = "chu27a8",
      sourceIdentifier = sourceIdentifier,
      identifiers = List(),
      locations = List(location)
    )
    val workWithCopyright = IdentifiedWork(
      title = Some("A scarf on a squirrel"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      canonicalId = "yxh928a",
      items = List(item))

    val actualJson = objectMapper.writeValueAsString(
      DisplayWork(workWithCopyright, WorksIncludes(items = true)))
    val expectedJson = s"""{
                          |     "type": "Work",
                          |     "id": "${workWithCopyright.canonicalId}",
                          |     "title": "${workWithCopyright.title.get}",
                          |     "creators": [ ],
                          |     "subjects": [ ],
                          |     "genres": [ ],
                          |     "publishers": [ ],
                          |     "placesOfPublication": [ ],
                          |     "items": [
                          |       {
                          |         "id": "${item.canonicalId}",
                          |         "type": "${item.ontologyType}",
                          |         "locations": [
                          |           {
                          |             "type": "${location.ontologyType}",
                          |             "url": "",
                          |             "locationType": "${location.locationType}",
                          |             "license": ${license(location.license)},
                          |             "credit": "${location.credit.get}"
                          |           }
                          |         ]
                          |       }
                          |     ]
                          |   }""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("includes subject information in DisplayWork serialisation") {
    val workWithSubjects = IdentifiedWork(
      title = Some("A seal selling seaweed sandwiches in Scotland"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = List(),
      canonicalId = "test_subject1",
      subjects = List(Concept("fish"), Concept("gardening"))
    )
    val actualJson =
      objectMapper.writeValueAsString(DisplayWork(workWithSubjects))
    val expectedJson = s"""{
                          |     "type": "Work",
                          |     "id": "${workWithSubjects.canonicalId}",
                          |     "title": "${workWithSubjects.title.get}",
                          |     "creators": [],
                          |     "subjects": [ ${concepts(
                            workWithSubjects.subjects)} ],
                          |     "genres": [ ],
                          |     "publishers": [ ],
                          |     "placesOfPublication": [ ]
                          |   }""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("includes genre information in DisplayWork serialisation") {
    val workWithSubjects = IdentifiedWork(
      title = Some("A guppy in a greenhouse"),
      sourceIdentifier = sourceIdentifier,
      version = 1,
      identifiers = List(),
      canonicalId = "test_subject1",
      genres = List(Concept("woodwork"), Concept("etching"))
    )
    val actualJson =
      objectMapper.writeValueAsString(DisplayWork(workWithSubjects))
    val expectedJson = s"""
                          |{
                          |     "type": "Work",
                          |     "id": "${workWithSubjects.canonicalId}",
                          |     "title": "${workWithSubjects.title.get}",
                          |     "creators": [],
                          |     "subjects": [ ],
                          |     "genres": [ ${concepts(workWithSubjects.genres)} ],
                          |     "publishers": [ ],
                          |     "placesOfPublication": [ ]
                          |   }""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)

  }

  it(
    "includes a list of identifiers on DisplayWork") {
    val srcIdentifier = SourceIdentifier(
      identifierScheme = IdentifierSchemes.miroImageNumber,
      ontologyType = "Work",
      value = "Test1234"
    )
    val work = workWith(
      canonicalId = "1234",
      title = "An insect huddled in an igloo",
      identifiers = List(srcIdentifier)
    )
    val actualJson = objectMapper.writeValueAsString(DisplayWork(work, WorksIncludes(identifiers = true)))
    val expectedJson = s"""
                          |{
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "title": "${work.title.get}",
                          | "creators": [ ],
                          | "identifiers": [ ${identifier(srcIdentifier)} ],
                          | "subjects": [ ],
                          | "genres": [ ],
                          | "publishers": [ ],
                          | "placesOfPublication": [ ]
                          |}
          """.stripMargin
      assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it(
    "always includes 'identifiers' with the identifiers include, even if there are no identifiers") {
    val work = workWith(
      canonicalId = "a87na87",
      title = "Idling inkwells of indigo images",
      identifiers = List()
    )
    val actualJson = objectMapper.writeValueAsString(DisplayWork(work, WorksIncludes(identifiers = true)))
    val expectedJson = s"""
                          |{
                          | "type": "Work",
                          | "id": "${work.canonicalId}",
                          | "title": "${work.title.get}",
                          | "creators": [ ],
                          | "identifiers": [ ],
                          | "subjects": [ ],
                          | "genres": [ ],
                          | "publishers": [ ],
                          | "placesOfPublication": [ ]
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
        locationType = "thumbnail-image",
        url = "https://iiif.example.org/1234/default.jpg",
        license = License_CCBY
      )
    )
    val actualJson = objectMapper.writeValueAsString(DisplayWork(work, WorksIncludes(thumbnail = true)))
    val expectedJson = s"""
                          |   {
                          |     "type": "Work",
                          |     "id": "${work.canonicalId}",
                          |     "title": "${work.title.get}",
                          |     "creators": [ ],
                          |     "subjects": [ ],
                          |     "genres": [ ],
                          |     "publishers": [ ],
                          |     "placesOfPublication": [ ],
                          |     "thumbnail": ${location(work.thumbnail.get)}
                          |   }
          """.stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }
}
