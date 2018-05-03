package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal.{Concept, Genre, Place}

class DisplayGenreV2SerialisationTest extends FunSpec
  with JsonMapperTestUtil {

  it("serialises a DisplayGenre constructed from a Genre correctly") {
    assertObjectMapsToJson(
      DisplayGenre(Genre("genreLabel", List(Concept("conceptLabel"), Place("placeLabel")))),
      expectedJson = s"""
         |  {
         |    "label" : "genreLabel",
         |    "concepts" : [
         |      {
         |        "label" : "conceptLabel",
         |        "type" : "Concept"
         |      },
         |      {
         |        "label" : "placeLabel",
         |        "type" : "Place"
         |      }
         |    ],
         |    "type" : "Genre"
         |  }
          """.stripMargin)
  }
}
