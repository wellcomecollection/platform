package uk.ac.wellcome.platform.transformer.sierra.transformers.sierra.subjects

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.transformer.sierra.generators.SierraDataGenerators
import uk.ac.wellcome.platform.transformer.sierra.source.{MarcSubfield, VarField}

class SierraOrganisationSubjectsTest extends FunSpec with Matchers with SierraDataGenerators {
  it("returns an empty list if there are no instances of MARC tag 610") {
    val bibData = createSierraBibDataWith(varFields = List())
    transformer.getSubjectsWithOrganisation(bibData) shouldBe List()
  }

  it("uses subfields a, b, c, d and e as the label") {
    val bibData = createSierraBibDataWith(
      varFields = List(
        VarField(
          marcTag = "610",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "a", content = "United States."),
            MarcSubfield(tag = "b", content = "Supreme Court,"),
            MarcSubfield(tag = "c", content = "Washington, DC."),
            MarcSubfield(tag = "d", content = "September 29, 2005,"),
            MarcSubfield(tag = "e", content = "depicted.")
          )
        )
      )
    )

    val subjects = transformer.getSubjectsWithOrganisation(bibData)
    subjects should have size 1

    subjects.head.label shouldBe "United States. Supreme Court, Washington, DC. September 29, 2005, depicted."
  }

  val transformer = new SierraOrganisationSubjects {}
}
