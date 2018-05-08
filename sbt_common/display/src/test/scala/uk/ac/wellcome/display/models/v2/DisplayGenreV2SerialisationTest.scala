package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.DisplaySerialisationTestBase
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal._

class DisplayGenreV2SerialisationTest
  extends FunSpec
  with DisplaySerialisationTestBase
  with JsonMapperTestUtil {

  it("serialises a DisplayGenre constructed from a Genre correctly") {
    val genre = Genre(
      label = "genreLabel",
      concepts = List(
        Unidentifiable(Concept("conceptLabel")),
        Unidentifiable(Place("placeLabel")),
        Identified(
          canonicalId = "sqwyavpj",
          identifiers = List(SourceIdentifier(
            identifierScheme = IdentifierSchemes.libraryOfCongressNames,
            value = "lcsh/sqw",
            ontologyType = "Period"
          )),
          agent = Period("periodLabel")
        )
      )
    )

    assertObjectMapsToJson(
      DisplayGenre(genre),
      expectedJson = s"""
         |  {
         |    "label" : "${genre.label}",
         |    "concepts" : [
         |      {
         |        "label" : "${genre.concepts(0).agent.label}",
         |        "type" : "${genre.concepts(0).agent.ontologyType}"
         |      },
         |      {
         |        "label" : "${genre.concepts(1).agent.label}",
         |        "type" : "${genre.concepts(1).agent.ontologyType}"
         |      },
         |      {
         |        "id": "${genre.concepts(2).canonicalId}",
         |        "identifiers": [${identifier(genre.concepts(2).identifiers(0))}],
         |        "label" : "${genre.concepts(2).agent.label}",
         |        "type" : "${genre.concepts(2).agent.ontologyType}"
         |      }
         |    ],
         |    "type" : "${genre.ontologyType}"
         |  }
          """.stripMargin
    )
  }
}
