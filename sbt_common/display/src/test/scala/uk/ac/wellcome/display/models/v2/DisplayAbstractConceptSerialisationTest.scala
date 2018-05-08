package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.DisplaySerialisationTestBase
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal.{
  AbstractConcept,
  Concept,
  Period,
  Place
}

class DisplayAbstractConceptSerialisationTest
    extends FunSpec
    with DisplaySerialisationTestBase
    with JsonMapperTestUtil {

  it("constructs a DisplayConcept from a Concept correctly") {
    assertObjectMapsToJson(
      DisplayConcept(Unidentifiable(Concept("conceptLabel"))),
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
      DisplayPeriod(Unidentifiable(Period("periodLabel"))),
      expectedJson = s"""
         |  {
         |    "label" : "periodLabel",
         |    "type"  : "Period"
         |  }
          """.stripMargin
    )
  }

  it("serialises a DisplayPlace constructed from a Place correctly") {
    assertObjectMapsToJson(
      DisplayPlace(Unidentifiable(Place("placeLabel"))),
      expectedJson = s"""
         |  {
         |    "label" : "placeLabel",
         |    "type"  : "Place"
         |  }
         """.stripMargin
    )
  }

  it("constructs a DisplayConcept from an identified") {
    val concept = Identified(
      id = "uq4bt5us",
      identifiers = List(SourceIdentifier(
        identifierScheme = IdentifierSchemes.libraryOfCongressNames,
        ontologyType = "Concept",
        value = "lcsh/uq4"
      ))
      agent = Concept("conceptLabel")
    )

    assertObjectMapsToJson(
      DisplayConcept(concept),
      expectedJson = s"""
         |  {
         |    "id": "${concept.id}",
         |    "identifiers": [${concept.identifiers}],
         |    "label" : "${concept.agent.label}",
         |    "type"  : "${concept.agent.ontologyType}"
         |  }
          """.stripMargin
    )
  }

  it("serialises a DisplayPeriod constructed from a Period correctly") {
    assertObjectMapsToJson(
      DisplayPeriod(Unidentifiable(Period("periodLabel"))),
      expectedJson = s"""
         |  {
         |    "label" : "periodLabel",
         |    "type"  : "Period"
         |  }
          """.stripMargin
    )
  }

  it("serialises a DisplayPlace constructed from a Place correctly") {
    assertObjectMapsToJson(
      DisplayPlace(Unidentifiable(Place("placeLabel"))),
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
        Unidentifiable(Concept("conceptLabel")),
        Unidentifiable(Place("placeLabel")),
        Unidentifiable(Period("periodLabel"))
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
