package uk.ac.wellcome.work_model

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil._

class ConceptTest extends FunSpec with Matchers with JsonTestUtil {

  val concept = Concept(label = "Woodwork")
  val expectedJson =
    s"""{
        "ontologyType": "Concept",
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
