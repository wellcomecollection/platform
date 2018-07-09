package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.IdentifiersUtil

class DisplayGenreV2SerialisationTest
    extends FunSpec
    with DisplayV2SerialisationTestBase
    with JsonMapperTestUtil
    with IdentifiersUtil {

  it("serialises a DisplayGenre constructed from a Genre correctly") {
    val concept0 = Unidentifiable(Concept("conceptLabel"))
    val concept1 = Unidentifiable(Place("placeLabel"))
    val concept2 = Identified(
      canonicalId = createCanonicalId,
      sourceIdentifier = createSourceIdentifierWith(
        ontologyType = "Period"
      ),
      agent = Period("periodLabel")
    )

    val genre = Genre(
      label = "genreLabel",
      concepts = List(concept0, concept1, concept2)
    )

    assertObjectMapsToJson(
      DisplayGenre(genre, includesIdentifiers = true),
      expectedJson = s"""
         |  {
         |    "label" : "${genre.label}",
         |    "concepts" : [
         |      {
         |        "label" : "${concept0.agent.label}",
         |        "type" : "${concept0.agent.ontologyType}"
         |      },
         |      {
         |        "label" : "${concept1.agent.label}",
         |        "type" : "${concept1.agent.ontologyType}"
         |      },
         |      {
         |        "id": "${concept2.canonicalId}",
         |        "identifiers": [${identifier(concept2.identifiers(0))}],
         |        "label" : "${concept2.agent.label}",
         |        "type" : "${concept2.agent.ontologyType}"
         |      }
         |    ],
         |    "type" : "${genre.ontologyType}"
         |  }
          """.stripMargin
    )
  }
}
