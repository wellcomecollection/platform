package uk.ac.wellcome.display.models.v1

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Guice
import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.{DisplaySerialisationTestBase, WorksUtil}
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.models._
import uk.ac.wellcome.test.utils.JsonTestUtil

class DisplayCreatorsV1SerialisationTest
    extends FunSpec
    with DisplaySerialisationTestBase
    with JsonTestUtil
    with WorksUtil {

  val injector = Guice.createInjector(DisplayJacksonModule)

  val objectMapper = injector.getInstance(classOf[ObjectMapper])

  it("serialises creators with a mixture of agents/organisations/persons") {

    val agent0 = Agent("Vivian Violet")
    val agent1 = Organisation("Verily Volumes")
    val agent2 = Person(
      label = "Havelock Vetinari",
      prefixes = Some(List("Lord Patrician")),
      numeration = Some("I"))
    )

    val work = IdentifiedWork(
      canonicalId = "v9w6cz66",
      sourceIdentifier = sourceIdentifier,
      version = 1,
      title = Some("Vultures vying for victory"),
      contributors = List(
        Unidentifiable(Contributor(agent = agent0)),
        Unidentifiable(Contributor(agent = agent1)),
        Unidentifiable(Contributor(agent = agent2))
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
                            |    ${agent(agent0)},
                            |    ${organisation(agent1)},
                            |    ${person(agent2)}
                            |  ],
                            |  "subjects": [ ],
                            |  "genres": [ ],
                            |  "publishers": [],
                            |  "placesOfPublication": [ ]
                            |}""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("serialises identified creators") {

    val agent0 = Person(
      label = "Havelock Vetinari",
      prefixes = Some(List("Lord Patrician")),
      numeration = Some("I")
    )
    val agent1 = Organisation(label = "Unseen University")
    val agent2 = Agent(label = "The Librarian")

    val work = IdentifiedWork(
      canonicalId = "v9w6cz66",
      sourceIdentifier = sourceIdentifier,
      version = 1,
      title = Some("Vultures vying for victory"),
      contributors = List(
        Identified(
          Contributor(agent = agent0),
          canonicalId = "hgfedcba",
          identifiers = List(
            SourceIdentifier(
              IdentifierSchemes.libraryOfCongressNames,
              ontologyType = "Organisation",
              value = "hv"))
        ),
        Identified(
          Contributor(agent = agent1),
          canonicalId = "abcdefgh",
          identifiers = List(
            SourceIdentifier(
              IdentifierSchemes.libraryOfCongressNames,
              ontologyType = "Organisation",
              value = "uu"))
        ),
        Identified(
          Contributor(agent = agent2),
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
                            |    ${person(agent0)},
                            |    ${organisation(agent1)},
                            |    ${agent(agent2)}
                            |  ],
                            |  "subjects": [ ],
                            |  "genres": [ ],
                            |  "publishers": [],
                            |  "placesOfPublication": [ ]
                            |}""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

}
