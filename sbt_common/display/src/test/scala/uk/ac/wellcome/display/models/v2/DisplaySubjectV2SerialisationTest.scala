package uk.ac.wellcome.display.models.v2

import org.scalatest.FunSpec
import uk.ac.wellcome.display.json.DisplayJsonUtil._
import uk.ac.wellcome.display.test.util.JsonMapperTestUtil
import uk.ac.wellcome.models.work.generators.IdentifiersGenerators
import uk.ac.wellcome.models.work.internal._

class DisplaySubjectV2SerialisationTest
    extends FunSpec
    with DisplayV2SerialisationTestBase
    with JsonMapperTestUtil
    with IdentifiersGenerators {

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
         |    "type" : "${subject.ontologyType}"
         |  }
          """.stripMargin
    )
  }

  it("serialises a DisplaySubject from a Subject with a Person concept") {
    val person = Person("Dolly Parton")
    val subject = Subject(
      label = "subjectLabel",
      concepts = List(Unidentifiable(person))
    )
    assertObjectMapsToJson(
      DisplaySubject(subject, includesIdentifiers = true),
      expectedJson = s"""
                        |  {
                        |    "label" : "${subject.label}",
                        |    "concepts" : [
                        |      {
                        |        "label" : "${person.label}",
                        |        "type" : "${ontologyType(person)}"
                        |      }],
                        |    "type" : "${subject.ontologyType}"
                        |  }
          """.stripMargin
    )
  }

  it("serialises a DisplaySubject from a Subject with a Agent concept") {
    val agent = Agent("Dolly Parton")
    val subject = Subject(
      label = "subjectLabel",
      concepts = List(Unidentifiable(agent))
    )
    assertObjectMapsToJson(
      DisplaySubject(subject, includesIdentifiers = true),
      expectedJson = s"""
                        |  {
                        |    "label" : "${subject.label}",
                        |    "concepts" : [
                        |      {
                        |        "label" : "${agent.label}",
                        |        "type" : "${ontologyType(agent)}"
                        |      }],
                        |    "type" : "${subject.ontologyType}"
                        |  }
          """.stripMargin
    )
  }

  it("serialises a DisplaySubject from a Subject with a Organisation concept") {
    val organisation = Organisation("Dolly Parton")
    val subject = Subject(
      label = "subjectLabel",
      concepts = List(Unidentifiable(organisation))
    )
    assertObjectMapsToJson(
      DisplaySubject(subject, includesIdentifiers = true),
      expectedJson = s"""
                        |  {
                        |    "label" : "${subject.label}",
                        |    "concepts" : [
                        |      {
                        |        "label" : "${organisation.label}",
                        |        "type" : "${ontologyType(organisation)}"
                        |      }],
                        |    "type" : "${subject.ontologyType}"
                        |  }
          """.stripMargin
    )
  }
}
