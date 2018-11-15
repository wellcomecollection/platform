package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.json.DisplayJsonUtil._
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.generators.IdentifiersGenerators
import uk.ac.wellcome.models.work.internal._

class DisplayAbstractConceptSerialisationTest
    extends FunSpec
    with DisplayV2SerialisationTestBase
    with JsonMapperTestUtil
    with IdentifiersGenerators {

  it("serialises an unidentified DisplayConcept") {
    assertObjectMapsToJson(
      DisplayConcept(
        id = None,
        identifiers = None,
        label = "conceptLabel"
      ),
      expectedJson = s"""
         |  {
         |    "label" : "conceptLabel",
         |    "type"  : "Concept"
         |  }
          """.stripMargin
    )
  }

  it("serialises an unidentified DisplayPeriod") {
    assertObjectMapsToJson(
      DisplayPeriod(
        id = None,
        identifiers = None,
        label = "periodLabel"
      ),
      expectedJson = s"""
         |  {
         |    "label" : "periodLabel",
         |    "type"  : "Period"
         |  }
          """.stripMargin
    )
  }

  it("serialises an unidentified DisplayPlace") {
    assertObjectMapsToJson(
      DisplayPlace(
        id = None,
        identifiers = None,
        label = "placeLabel"
      ),
      expectedJson = s"""
         |  {
         |    "label" : "placeLabel",
         |    "type"  : "Place"
         |  }
         """.stripMargin
    )
  }

  it("constructs a DisplayConcept from an identified Concept") {
    val concept = Identified(
      canonicalId = "uq4bt5us",
      sourceIdentifier = createSourceIdentifierWith(
        ontologyType = "Concept"
      ),
      agent = Concept("conceptLabel")
    )

    assertObjectMapsToJson(
      DisplayAbstractConcept(concept, includesIdentifiers = true),
      expectedJson = s"""
         |  {
         |    "id": "${concept.canonicalId}",
         |    "identifiers": [${identifier(concept.identifiers(0))}],
         |    "label" : "${concept.agent.label}",
         |    "type"  : "Concept"
         |  }
          """.stripMargin
    )
  }

  it("serialises AbstractDisplayConcepts constructed from AbstractConcepts") {
    assertObjectMapsToJson(
      List[Displayable[AbstractConcept]](
        Unidentifiable(Concept("conceptLabel")),
        Unidentifiable(Place("placeLabel")),
        Unidentifiable(Period("periodLabel"))
      ).map(DisplayAbstractConcept(_, includesIdentifiers = false)),
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
