package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.DisplaySerialisationTestBase
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.WorksUtil

class DisplayPublishersV2SerialisationTest
    extends FunSpec
    with DisplaySerialisationTestBase
    with JsonMapperTestUtil
    with WorksUtil {

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
    val displayWork = DisplayWorkV2(work)

    val actualJson = objectMapper.writeValueAsString(displayWork)
    val expectedJson = s"""
                            |{
                            |  "type": "Work",
                            |  "id": "${work.canonicalId}",
                            |  "title": "${work.title.get}",
                            |  "contributors": [ ],
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
