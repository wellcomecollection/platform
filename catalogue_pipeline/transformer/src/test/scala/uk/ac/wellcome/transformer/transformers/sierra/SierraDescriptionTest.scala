package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{Agent, IdentifierSchemes, SourceIdentifier}
import uk.ac.wellcome.test.utils.SierraData
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData, VarField}

class SierraDescriptionTest extends FunSpec with Matchers with SierraData {

  it("extracts a work description where MARC field 520 with subfield a is populated") {
    val description = "A panolopy of penguins perching on a python."

    assertFindsCorrectDescription(
      varFields = List(
        VarField(
          fieldTag = "?",
          marcTag = "520",
          indicator1 = " ",
          indicator2 = " ",
          subfields = List(
            MarcSubfield(
              tag = "a",
              content = description
            )
          )
        )
      ),
      expectedDescription = Some(description)
    )
  }

  it("extracts a work description where MARC field 520 with subfield a and b are populated") {
    val description = "A panolopy of penguins perching on a python."
    val summaryDescription = "A fracas of frolicking frogs on futons."

    assertFindsCorrectDescription(
      varFields = List(
        VarField(
          fieldTag = "?",
          marcTag = "520",
          indicator1 = " ",
          indicator2 = " ",
          subfields = List(
            MarcSubfield(
              tag = "a",
              content = description
            ),
            MarcSubfield(
              tag = "b",
              content = summaryDescription
            )
          )
        )
      ),
      expectedDescription = Some(s"$description $summaryDescription")
    )
  }

  val transformer = new SierraDescription {}

  private def assertFindsCorrectDescription(
    varFields: List[VarField],
    expectedDescription: Option[String]
  ) = {

    val bibData = SierraBibData(
      id = "b1234567",
      title = "A page of pedantic plans, prancing playfully.",
      varFields = varFields
    )

    transformer.getDescription(bibData = bibData) shouldBe expectedDescription
  }
}
