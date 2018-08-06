package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.test.util.IdentifiersUtil

class DisplaySubjectV2SerialisationTest
    extends FunSpec
    with DisplayV2SerialisationTestBase
    with JsonMapperTestUtil
    with IdentifiersUtil {

  it("serialises a DisplaySubject constructed from a Subject") {
    val concept0 = Unidentifiable(Concept("conceptLabel"))
    val concept1 = Unidentifiable(Period("periodLabel"))
    val concept2 = Identified(
      canonicalId = createCanonicalId,
      sourceIdentifier = createSourceIdentifierWith(
        ontologyType = "Place"
      ),
      agent = Place("placeLabel")
    )

    val subject = Subject(
      label = "subjectLabel",
      concepts = List(concept0, concept1, concept2)
    )

    assertObjectMapsToJson(
      DisplaySubject(subject, includesIdentifiers = true),
      expectedJson = s"""
         |  {
         |    "label" : "${subject.label}",
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
         |    "type" : "${subject.ontologyType}"
         |  }
          """.stripMargin
    )
  }
}
