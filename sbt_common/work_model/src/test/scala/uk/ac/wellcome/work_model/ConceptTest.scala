package uk.ac.wellcome.work_model

import org.scalatest.{FunSpec, Matchers}

class ConceptTest extends FunSpec with Matchers with JsonTestUtil {

  val concept = Concept(label = "Woodwork"),
  val expectedJson = s"""{
        "type": "Concept",
        "ontologyType": "Concept",
        "label": "Woodwork",
        "qualifierType": null
      }"""

  it("serialises Concepts to JSON") {
      val actualJson = toJson(concept).get
      assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("deserialises JSON as Concepts") {
      val parsedConcept = fromJson[AbstractConcept](expectedJson).get
      parsedConcept shouldBe concept
  }
}
