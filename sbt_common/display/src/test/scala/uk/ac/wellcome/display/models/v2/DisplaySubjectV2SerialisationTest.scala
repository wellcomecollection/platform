package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.models.DisplaySerialisationTestBase
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal._

class DisplaySubjectV2SerialisationTest
    extends FunSpec
    with JsonMapperTestUtil {

  it("serialises a DisplaySubject constructed from a Subject correctly") {
    val subject = Subject(
      label = "subjectLabel",
      concepts = List(
        Unidentifiable(Concept("conceptLabel")),
        Unidentifiable(Period("periodLabel")),
        Identified(
          canonicalId = "p4xe8u22",
          identifiers = List(SourceIdentifier(
            identifierScheme = IdentifierSchemes.libraryOfCongressNames,
            value = "lcsh/p4x",
            ontologyType = "Place"
          )),
          agent = Place("placeLabel")
        )
      )
    )

    assertObjectMapsToJson(
      DisplaySubject(subject),
      expectedJson = s"""
         |  {
         |    "label" : "${subject.label}",
         |    "concepts" : [
         |      {
         |        "label" : "${subject.concepts(0).agent.label}",
         |        "type" : "${subject.concepts(0).agent.ontologyType}"
         |      },
         |      {
         |        "label" : "${subject.concepts(1).agent.label}",
         |        "type" : "${subject.concepts(1).agent.ontologyType}"
         |      },
         |      {
         |        "id": "${subject.concepts(2).canonicalId}",
         |        "identifiers": [${identifier(subject.concepts(2).identifiers(0))}],
         |        "label" : "${subject.concepts(2).agent.label}",
         |        "type" : "${subject.concepts(2).agent.ontologyType}"
         |      }
         |    ],
         |    "type" : "${subject.ontologyType}"
         |  }
          """.stripMargin
    )
  }
}
