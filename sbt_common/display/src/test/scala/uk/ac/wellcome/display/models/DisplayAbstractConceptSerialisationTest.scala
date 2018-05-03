package uk.ac.wellcome.display.models

import org.scalatest.FunSpec
import uk.ac.wellcome.models.work.internal.{
  AbstractConcept,
  Concept,
  Period,
  Place
}

class DisplayAbstractConceptSerialisationTest
    extends FunSpec
    with JsonMapperTestUtil {

  it("constructs a DisplayConcept from a Concept correctly") {
    assertObjectMapsToJson(
      DisplayConcept(Concept("conceptLabel")),
      expectedJson = s"""
         |  {
         |    "label" : "conceptLabel",
         |    "type"  : "Concept"
         |  }
          """.stripMargin
    )
  }

  it("serialises a DisplayPeriod constructed from a Period correctly") {
    assertObjectMapsToJson(
      DisplayPeriod(Period("periodLabel")),
      expectedJson = s"""
         |  {
         |    "label" : "periodLabel",
         |    "type"  : "Period"
         |  }
          """.stripMargin
    )
  }

  it("serialises a DisplayPlace constructed from a place correctly") {
    assertObjectMapsToJson(
      DisplayPlace(Place("placeLabel")),
      expectedJson = s"""
         |  {
         |    "label" : "placeLabel",
         |    "type"  : "Place"
         |  }
         """.stripMargin
    )
  }

  it(
    "serialises AbstractDisplayConcepts constructed from AbstractConcepts correctly") {
    assertObjectMapsToJson(
      List[AbstractConcept](
        Concept("conceptLabel"),
        Place("placeLabel"),
        Period("periodLabel")
      ).map(DisplayAbstractConcept(_)),
      expectedJson = s"""
          | [
          |    {
          |      "label" : "conceptLabel",
          |      "type"  : "Concept"
          |    },
          |    {
          |      "label" : "placeLabel",
          |      "type"  : "Place"
          |    },
          |    {
          |      "label" : "periodLabel",
          |      "type"  : "Period"
          |    }
          |  ]
          """.stripMargin
    )
  }
}
