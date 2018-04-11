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

  it("serialises a DisplayWork correctly") {

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

  describe("creators") {
    it("serialises creators with a mixture of agents/organisations/persons") {
      val work = IdentifiedWork(
        canonicalId = "v9w6cz66",
        sourceIdentifier = sourceIdentifier,
        version = 1,
        title = Some("Vultures vying for victory"),
        creators = List(
          Unidentifiable(Agent("Vivian Violet")),
          Unidentifiable(Organisation("Verily Volumes")),
          Unidentifiable(
            Person(
              label = "Havelock Vetinari",
              prefix = Some("Lord Patrician"),
              numeration = Some("I")))
        )
      )
      val displayWork = DisplayWork(work)

      val actualJson = objectMapper.writeValueAsString(displayWork)
      val expectedJson = s"""
        |{
        |  "type": "Work",
        |  "id": "${work.canonicalId}",
        |  "title": "${work.title.get}",
        |  "creators": [
        |    ${identifiedOrUnidentifiable(work.creators(0), abstractAgent)},
        |    ${identifiedOrUnidentifiable(work.creators(1), abstractAgent)},
        |    ${identifiedOrUnidentifiable(work.creators(2), abstractAgent)}
        |  ],
        |  "subjects": [ ],
        |  "genres": [ ],
        |  "publishers": [],
        |  "placesOfPublication": [ ]
        |}""".stripMargin

      assertJsonStringsAreEqual(actualJson, expectedJson)
    }

    it("serialises identified creators") {
      val work = IdentifiedWork(
        canonicalId = "v9w6cz66",
        sourceIdentifier = sourceIdentifier,
        version = 1,
        title = Some("Vultures vying for victory"),
        creators = List(
          Identified(
            Person(
              label = "Havelock Vetinari",
              prefix = Some("Lord Patrician"),
              numeration = Some("I")),
            canonicalId = "hgfedcba",
            identifiers = List(
              SourceIdentifier(
                IdentifierSchemes.libraryOfCongressNames,
                ontologyType = "Organisation",
                value = "hv"))
          ),
          Identified(
            Organisation(label = "Unseen University"),
            canonicalId = "abcdefgh",
            identifiers = List(
              SourceIdentifier(
                IdentifierSchemes.libraryOfCongressNames,
                ontologyType = "Organisation",
                value = "uu"))
          ),
          Identified(
            Agent(label = "The Librarian"),
            canonicalId = "blahbluh",
            identifiers = List(
              SourceIdentifier(
                IdentifierSchemes.libraryOfCongressNames,
                ontologyType = "Organisation",
                value = "uu"))
          )
        )
      )
      val displayWork = DisplayWork(work)

      val actualJson = objectMapper.writeValueAsString(displayWork)
      val expectedJson = s"""
                             |{
                             |  "type": "Work",
                             |  "id": "${work.canonicalId}",
                             |  "title": "${work.title.get}",
                             |  "creators": [
                             |    ${identifiedOrUnidentifiable(work.creators(0), abstractAgent)},
                             |    ${identifiedOrUnidentifiable(work.creators(1), abstractAgent)},
                             |    ${identifiedOrUnidentifiable(work.creators(2), abstractAgent)}
                             |  ],
                             |  "subjects": [ ],
                             |  "genres": [ ],
                             |  "publishers": [],
                             |  "placesOfPublication": [ ]
                             |}""".stripMargin

      assertJsonStringsAreEqual(actualJson, expectedJson)
    }
  }

  describe("locations") {
    it("serialises a physical location correctly") {
      val physicalLocation = PhysicalLocation(
        locationType = "smeg",
        label = "a stack of slick slimes"
      )

      val work = IdentifiedWork(
        canonicalId = "zm9q6c6h",
        sourceIdentifier = sourceIdentifier,
        version = 1,
        title = Some("A zoo of zebras doing zumba"),
        items = List(
          IdentifiedItem(
            canonicalId = "mhberjwy7",
            sourceIdentifier = sourceIdentifier,
            locations = List(physicalLocation)
          )
        )
      )
      val displayWork = DisplayWork(work, includes = WorksIncludes(items = true))

      val actualJson = objectMapper.writeValueAsString(displayWork)
      val expectedJson = s"""
                             |{
                             |  "type": "Work",
                             |  "id": "${work.canonicalId}",
                             |  "title": "${work.title.get}",
                             |  "creators": [ ],
                             |  "items": [ ${items(work.items)} ],
                             |  "subjects": [ ],
                             |  "genres": [ ],
                             |  "publishers": [],
                             |  "placesOfPublication": [ ]
                             |}""".stripMargin

      assertJsonStringsAreEqual(actualJson, expectedJson)
    }
  }

  describe("place of publication") {
    it("serialises the placesOfPublication field") {
      val work = IdentifiedWork(
        canonicalId = "avfpwgrr",
        sourceIdentifier = sourceIdentifier,
        title = Some("Ahoy!  Armoured angelfish are attacking the armada!"),
        placesOfPublication = List(Place("Durmstrang")),
        version = 1
      )
      val displayWork = DisplayWork(work)

      val actualJson = objectMapper.writeValueAsString(displayWork)
      val expectedJson = s"""
                             |{
                             |  "type": "Work",
                             |  "id": "${work.canonicalId}",
                             |  "title": "${work.title.get}",
                             |  "creators": [ ],
                             |  "subjects": [ ],
                             |  "genres": [ ],
                             |  "publishers": [],
                             |  "placesOfPublication": [
                             |    {
                             |      "label": "${work.placesOfPublication.head.label}",
                             |      "type": "Place"
                             |    }
                             |  ]
                             |
                             |}""".stripMargin

      assertJsonStringsAreEqual(actualJson, expectedJson)
    }
  }

  describe("publication date") {
    it("omits the publicationDate field if it is empty") {
      val work = IdentifiedWork(
        canonicalId = "arfj5cj4",
        sourceIdentifier = sourceIdentifier,
        title = Some("Asking aging armadillos for another appraisal"),
        publicationDate = None,
        version = 1
      )
      val displayWork = DisplayWork(work)

      val actualJson = objectMapper.writeValueAsString(displayWork)
      val expectedJson = s"""
                            |{
                            |  "type": "Work",
                            |  "id": "${work.canonicalId}",
                            |  "title": "${work.title.get}",
                            |  "creators": [ ],
                            |  "subjects": [ ],
                            |  "genres": [ ],
                            |  "publishers": [],
                            |  "placesOfPublication": [ ]
                            |}""".stripMargin

      assertJsonStringsAreEqual(actualJson, expectedJson)
    }

    it("includes the publicationDate field if it is present on the Work") {
      val work = IdentifiedWork(
        canonicalId = "avfpwgrr",
        sourceIdentifier = sourceIdentifier,
        title = Some("Ahoy!  Armoured angelfish are attacking the armada!"),
        publicationDate = Some(Period("1923")),
        version = 1
      )
      val displayWork = DisplayWork(work)

      val actualJson = objectMapper.writeValueAsString(displayWork)
      val expectedJson = s"""
                            |{
                            |  "type": "Work",
                            |  "id": "${work.canonicalId}",
                            |  "title": "${work.title.get}",
                            |  "creators": [ ],
                            |  "subjects": [ ],
                            |  "genres": [ ],
                            |  "publishers": [],
                            |  "publicationDate": ${period(work.publicationDate.get)},
                            |  "placesOfPublication": [ ]
                            |}""".stripMargin

      assertJsonStringsAreEqual(actualJson, expectedJson)
    }
  }

  describe("publishers") {
    it(
      "includes the publishers field with a mixture of agents/organisations/persons") {
      val work = IdentifiedWork(
        canonicalId = "v9w6cz66",
        sourceIdentifier = sourceIdentifier,
        version = 1,
        title = Some("Vultures vying for victory"),
        publishers = List(
          Unidentifiable(Agent("Vivian Violet")),
          Unidentifiable(Organisation("Verily Volumes")),
          Unidentifiable(
            Person(
              label = "Havelock Vetinari",
              prefix = Some("Lord Patrician"),
              numeration = Some("I")))
        )
      )
      val displayWork = DisplayWork(work)

      val actualJson = objectMapper.writeValueAsString(displayWork)
      val expectedJson = s"""
                            |{
                            |  "type": "Work",
                            |  "id": "${work.canonicalId}",
                            |  "title": "${work.title.get}",
                            |  "creators": [ ],
                            |  "subjects": [ ],
                            |  "genres": [ ],
                            |  "publishers": [
                            |    ${identifiedOrUnidentifiable(work.publishers(0), abstractAgent)},
                            |    ${identifiedOrUnidentifiable(work.publishers(1), abstractAgent)},
                            |    ${identifiedOrUnidentifiable(work.publishers(2), abstractAgent)}
                            |  ],
                            |  "placesOfPublication": [ ]
                            |}""".stripMargin

      assertJsonStringsAreEqual(actualJson, expectedJson)
    }
  }
}
