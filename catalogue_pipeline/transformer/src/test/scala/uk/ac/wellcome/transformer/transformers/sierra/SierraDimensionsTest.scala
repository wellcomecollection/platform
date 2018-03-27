package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.transformer.source.{
  MarcSubfield,
  SierraBibData,
  VarField
}

class SierraDimensionsTest extends FunSpec with Matchers {

  val transformer = new SierraDimensions {}

  it("gets no dimensions if there is no MARC field 300 with subfield $$c") {
    val bibData = SierraBibData(id = "d1000001", varFields = List())
    transformer.getDimensions(bibData = bibData) shouldBe None
  }

  it("extracts dimensions from MARC field 300 subfield $$c") {
    val dimensions = "23cm"

    val varFields = List(
      VarField(
        fieldTag = "?",
        marcTag = "300",
        indicator1 = " ",
        indicator2 = " ",
        subfields = List(
          MarcSubfield(
            tag = "a",
            content = "149 p. ;"
          ),
          MarcSubfield(
            tag = "c",
            content = "23cm"
          )
        )
      )
    )

    val bibData = SierraBibData(id = "d2000002", varFields = varFields)
    transformer.getDimensions(bibData = bibData) shouldBe Some(dimensions)
  }

  it("extracts an dimensions where there are multiple MARC field 300 $$c") {
    val dimensions1 = "21 cm. +"
    val dimensions2 = "37cm"

    val expectedDimensions = s"$dimensions1 $dimensions2"

    val varFields = List(
      VarField(
        fieldTag = "?",
        marcTag = "300",
        indicator1 = " ",
        indicator2 = " ",
        subfields = List(
          MarcSubfield(
            tag = "a",
            content = "1 print :"
          ),
          MarcSubfield(
            tag = "c",
            content = dimensions1
          )
        )
      ),
      VarField(
        fieldTag = "?",
        marcTag = "300",
        indicator1 = " ",
        indicator2 = " ",
        subfields = List(
          MarcSubfield(
            tag = "c",
            content = dimensions2
          )
        )
      )
    )

    val bibData = SierraBibData(id = "d3000003", varFields = varFields)
    transformer.getDimensions(bibData = bibData) shouldBe Some(
      expectedDimensions)
  }
}
