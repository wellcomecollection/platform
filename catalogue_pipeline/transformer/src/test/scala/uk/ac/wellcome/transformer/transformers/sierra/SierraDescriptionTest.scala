package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.test.utils.SierraData
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData, VarField}

class SierraDescriptionTest extends FunSpec with Matchers with SierraData {

  it(
    "extracts a work description where MARC field 520 with subfield a is populated") {
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

  it("extracts a work description where there are multiple MARC field 520") {
    val description1 = "A malcontent marc minion."
    val description2 = "A fresh fishy fruit."
    val summaryDescription2 = "A case of colloidal coffee capsules."

    val description = s"$description1 $description2 $summaryDescription2"

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
              content = description1
            )
          )
        ),
        VarField(
          fieldTag = "?",
          marcTag = "520",
          indicator1 = " ",
          indicator2 = " ",
          subfields = List(
            MarcSubfield(
              tag = "a",
              content = description2
            ),
            MarcSubfield(
              tag = "b",
              content = summaryDescription2
            )
          )
        )
      ),
      expectedDescription = Some(description)
    )
  }

  it(
    "extracts a work description where MARC field 520 with subfield a and b are populated") {
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

  it("does not extract a work description where MARC field 520 is absent") {
    assertFindsCorrectDescription(
      varFields = List(
        VarField(
          fieldTag = "?",
          marcTag = "666",
          indicator1 = " ",
          indicator2 = " ",
          subfields = Nil
        )
      ),
      expectedDescription = None
    )
  }

  val transformer = new SierraDescription {}

  private def assertFindsCorrectDescription(
    varFields: List[VarField],
    expectedDescription: Option[String]
  ) = {

    val bibData = SierraBibData(
      id = "b1234567",
      title = Some("A xylophone full of xenon."),
      varFields = varFields
    )

    transformer.getDescription(bibData = bibData) shouldBe expectedDescription
  }
}
