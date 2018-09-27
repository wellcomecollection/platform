package uk.ac.wellcome.platform.transformer.sierra.transformers.sierra.subjects

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.transformer.sierra.generators.SierraDataGenerators

class SierraOrganisationSubjectsTest extends FunSpec with Matchers with SierraDataGenerators {
  it("returns an empty list if there are no instances of MARC tag 610") {
    val bibData = createSierraBibDataWith(varFields = List())
    transformer.getSubjectsWithOrganisation(bibData) shouldBe List()
  }

  val transformer = new SierraOrganisationSubjects {}
}
