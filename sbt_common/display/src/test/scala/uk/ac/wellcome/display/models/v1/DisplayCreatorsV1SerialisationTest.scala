package uk.ac.wellcome.display.models.v1

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Guice
import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.{DisplaySerialisationTestBase, WorksUtil}
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.work_model._

class DisplayCreatorsV1SerialisationTest
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
      contributors = List(
        Contributor(agent = Unidentifiable(Agent("Vivian Violet"))),
        Contributor(agent = Unidentifiable(Organisation("Verily Volumes"))),
        Contributor(
          agent = Unidentifiable(
            Person(
              label = "Havelock Vetinari",
              prefix = Some("Lord Patrician"),
              numeration = Some("I")
            )))
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
                            work.contributors(0).agent,
                            abstractAgent)},
                            |    ${identifiedOrUnidentifiable(
                            work.contributors(1).agent,
                            abstractAgent)},
                            |    ${identifiedOrUnidentifiable(
                            work.contributors(2).agent,
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
      contributors = List(
        Contributor(
          agent = Identified(
            Person(
              label = "Havelock Vetinari",
              prefix = Some("Lord Patrician"),
              numeration = Some("I")
            ),
            canonicalId = "hgfedcba",
            identifiers = List(
              SourceIdentifier(
                IdentifierSchemes.libraryOfCongressNames,
                ontologyType = "Organisation",
                value = "hv"
              )
            )
          )
        ),
        Contributor(
          agent = Identified(
            Organisation(label = "Unseen University"),
            canonicalId = "abcdefgh",
            identifiers = List(
              SourceIdentifier(
                IdentifierSchemes.libraryOfCongressNames,
                ontologyType = "Organisation",
                value = "uu"
              )
            )
          )
        ),
        Contributor(
          agent = Identified(
            Agent(label = "The Librarian"),
            canonicalId = "blahbluh",
            identifiers = List(
              SourceIdentifier(
                IdentifierSchemes.libraryOfCongressNames,
                ontologyType = "Organisation",
                value = "uu"
              )
            )
          )
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
                            work.contributors(0).agent,
                            abstractAgent)},
                            |    ${identifiedOrUnidentifiable(
                            work.contributors(1).agent,
                            abstractAgent)},
                            |    ${identifiedOrUnidentifiable(
                            work.contributors(2).agent,
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
