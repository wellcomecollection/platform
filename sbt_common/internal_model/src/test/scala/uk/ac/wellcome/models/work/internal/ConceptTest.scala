package uk.ac.wellcome.models.work.internal

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions

class ConceptTest extends FunSpec with Matchers with JsonAssertions {

  val concept = Concept(label = "Woodwork")
  val expectedJson =
    s"""{
        "label": "Woodwork"
      }"""

  it("serialises Concepts to JSON") {
    val actualJson = toJson(concept).get
    assertJsonStringsAreEqual(actualJson, expectedJson)
  }

  it("deserialises JSON as Concepts") {
    val parsedConcept = fromJson[Concept](expectedJson).get
    parsedConcept shouldBe concept
  }
}
