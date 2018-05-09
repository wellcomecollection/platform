package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.DisplaySerialisationTestBase
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal._

class DisplayAbstractConceptSerialisationTest
    extends FunSpec
    with DisplaySerialisationTestBase
    with JsonMapperTestUtil {

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
      identifiers = List(SourceIdentifier(
        identifierScheme = IdentifierSchemes.libraryOfCongressNames,
        ontologyType = "Concept",
        value = "lcsh/uq4"
      )),
      agent = Concept("conceptLabel")
    )

    assertObjectMapsToJson(
      DisplayAbstractConcept(concept),
      expectedJson = s"""
         |  {
         |    "id": "${concept.canonicalId}",
         |    "identifiers": [${identifier(concept.identifiers(0))}],
         |    "label" : "${concept.agent.label}",
         |    "type"  : "${concept.agent.ontologyType}"
         |  }
          """.stripMargin
    )
  }

  it(
    "serialises AbstractDisplayConcepts constructed from AbstractConcepts correctly") {
    assertObjectMapsToJson(
      List[Displayable[AbstractConcept]](
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
