package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData, VarField}

class SierraContributorsTest extends FunSpec with Matchers {

  val transformer = new SierraContributors {}

  it("gets an empty contributor list from empty bib data") {
    val bibData = SierraBibData(
      id = "3224766",
      title = None,
      varFields = List()
    )

    val contributors = transformer.getContributors(bibData)
    contributors shouldBe List()
  }
}
