package uk.ac.wellcome.display.models.v1

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Guice
import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.{DisplaySerialisationTestBase, WorksUtil}
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.models._
import uk.ac.wellcome.test.utils.JsonTestUtil

class DisplayPublishersSerialisationTest
    extends FunSpec
    with DisplaySerialisationTestBase
    with JsonTestUtil
    with WorksUtil {
  val injector = Guice.createInjector(DisplayJacksonModule)

  val objectMapper = injector.getInstance(classOf[ObjectMapper])

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
    val displayWork = DisplayWorkV1(work)

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
                            |    ${identifiedOrUnidentifiable(
                            work.publishers(0),
                            abstractAgent)},
                            |    ${identifiedOrUnidentifiable(
                            work.publishers(1),
                            abstractAgent)},
                            |    ${identifiedOrUnidentifiable(
                            work.publishers(2),
                            abstractAgent)}
                            |  ],
                            |  "placesOfPublication": [ ]
                            |}""".stripMargin

    assertJsonStringsAreEqual(actualJson, expectedJson)
  }
}
