package uk.ac.wellcome.models

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks._
import uk.ac.wellcome.test.utils.JsonTestUtil
import uk.ac.wellcome.utils.JsonUtil._

class ConceptTest extends FunSpec with Matchers with JsonTestUtil {

  val testCases = Table(
    ("concept", "expectedJson"),
    (
      Concept(label = "Woodwork"),
      s"""{
        "type": "Concept",
        "ontologyType": "Concept",
        "label": "Woodwork",
        "qualifierType": null
      }"""
    ),
    (
      QualifiedConcept(
        label = "Dangerous diseases",
        concept = Concept(
          label = "disease"
//          qualifiers = List(
//            Concept(
//              qualifierType = "general-subdivision",
//              label = "dispersion & direction"
//            )
//          )
        )
      ),
      s"""{
        "type": "QualifiedConcept",
        "ontologyType": "QualifiedConcept",
        "label": "Dangerous diseases",
        "concept": {
          "label": "disease",
          "ontologyType": "Concept",
          "qualifierType": null
        }
      }"""
    )
  )

  it("serialises Concepts to JSON") {
    forAll(testCases) { (concept: AbstractConcept, expectedJson: String) =>
      val actualJson = toJson(concept).get
      assertJsonStringsAreEqual(actualJson, expectedJson)
    }
  }

  it("deserialises JSON as Concepts") {
    forAll(testCases) { (concept: AbstractConcept, expectedJson: String) =>
      val parsedConcept = fromJson[AbstractConcept](expectedJson).get
      parsedConcept shouldBe concept
    }
  }
}
