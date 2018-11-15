package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.json.DisplayJsonUtil._
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.generators.IdentifiersGenerators
import uk.ac.wellcome.models.work.internal._

class DisplayGenreV2SerialisationTest
    extends FunSpec
    with DisplayV2SerialisationTestBase
    with JsonMapperTestUtil
    with IdentifiersGenerators {

  it("serialises a DisplayGenre constructed from a Genre") {
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
         |        "type" : "${ontologyType(concept0.agent)}"
         |      },
         |      {
         |        "label" : "${concept1.agent.label}",
         |        "type" : "${ontologyType(concept1.agent)}"
         |      },
         |      {
         |        "id": "${concept2.canonicalId}",
         |        "identifiers": [${identifier(concept2.identifiers(0))}],
         |        "label" : "${concept2.agent.label}",
         |        "type" : "${ontologyType(concept2.agent)}"
         |      }
         |    ],
         |    "type" : "${genre.ontologyType}"
         |  }
          """.stripMargin
    )
  }
}
