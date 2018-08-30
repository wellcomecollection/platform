package uk.ac.wellcome.display.models.v1

import org.scalatest.FunSpec
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.WorksGenerators

class DisplayCreatorsV1SerialisationTest
    extends FunSpec
    with DisplayV1SerialisationTestBase
    with JsonMapperTestUtil
    with WorksGenerators {

  it("serialises creators") {
    val work = createIdentifiedWorkWith(
      contributors = List(
        Contributor(agent = Unidentifiable(Agent("Vivian Violet"))),
        Contributor(agent = Unidentifiable(Agent("Verily Volumes"))),
        Contributor(
          agent = Unidentifiable(
            Agent(label = "Havelock Vetinari")
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
                            |  "title": "${work.title}",
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
