package uk.ac.wellcome.display.models.v1

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Guice
import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.{DisplaySerialisationTestBase, WorksUtil}
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.models._
import uk.ac.wellcome.test.utils.JsonTestUtil

class DisplayCreatorsSerialisationTest
    extends FunSpec
    with DisplaySerialisationTestBase
    with JsonTestUtil
    with WorksUtil {

  val injector = Guice.createInjector(DisplayJacksonModule)

  val objectMapper = injector.getInstance(classOf[ObjectMapper])

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
    val displayWork = DisplayWorkV1(work)

    val actualJson = objectMapper.writeValueAsString(displayWork)
    val expectedJson = s"""
                            |{
                            |  "type": "Work",
                            |  "id": "${work.canonicalId}",
                            |  "title": "${work.title.get}",
                            |  "creators": [
                            |    ${identifiedOrUnidentifiable(
                            work.creators(0),
                            abstractAgent)},
                            |    ${identifiedOrUnidentifiable(
                            work.creators(1),
                            abstractAgent)},
                            |    ${identifiedOrUnidentifiable(
                            work.creators(2),
                            abstractAgent)}
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
    val displayWork = DisplayWorkV1(work)

    val actualJson = objectMapper.writeValueAsString(displayWork)
    val expectedJson = s"""
                            |{
                            |  "type": "Work",
                            |  "id": "${work.canonicalId}",
                            |  "title": "${work.title.get}",
                            |  "creators": [
                            |    ${identifiedOrUnidentifiable(
                            work.creators(0),
                            abstractAgent)},
                            |    ${identifiedOrUnidentifiable(
                            work.creators(1),
                            abstractAgent)},
                            |    ${identifiedOrUnidentifiable(
                            work.creators(2),
                            abstractAgent)}
                            |  ],
                            |  "subjects": [ ],
                            |  "genres": [ ],
                            |  "publishers": [],
                            |  "placesOfPublication": [ ]
                            |}""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

}
