package uk.ac.wellcome.display.models

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks._
import uk.ac.wellcome.models.{AbstractConcept, Concept, QualifiedConcept}
import uk.ac.wellcome.utils.JsonUtil._

class DisplayConceptTest extends FunSpec with Matchers {

  val testCases = Table(
    ("internalConcept", "displayConcept"),

    (
      Concept(label = "painting"),
      DisplayConcept(label = "painting")
    ),
    (
      QualifiedConcept(
        label = "Dangerous diseases",
        concept = Concept(
          label = "disease",
          qualifiers = List(
            Concept(
              qualifierType = "general-subdivision",
              label = "dispersion & direction"
            )
          )
        )
      ),
      DisplayConcept(
        label = "Dangerous diseases",
        concept = Some(DisplayConcept(
          label = "disease",
          qualifiers = List(
            DisplayConcept(
              qualifierType = Some("general-subdivision"),
              label = "dispersion & direction"
            )
          )
        )),
        ontologyType = "QualifiedConcept"
      )
    )
  )

  it("converts instances of AbstractConcept to DisplayConcept") {
    forAll(testCases) { (internalConcept: AbstractConcept, displayConcept: DisplayConcept) =>
      val actualDisplayConcept = DisplayConcept(internalConcept)
      actualDisplayConcept shouldBe displayConcept
    }
  }
}
