package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.test.utils.SierraData
import uk.ac.wellcome.models.Period
import uk.ac.wellcome.transformer.source.{
  MarcSubfield,
  SierraBibData,
  VarField
}

class SierraPublicationDateTest extends FunSpec with Matchers with SierraData {

  val transformer = new SierraPublicationDate {}

  it("extracts publication date from MARC field 260 with subfield $c") {
    val varFields = List(
      VarField(
        fieldTag = "?",
        marcTag = "260",
        indicator1 = " ",
        indicator2 = " ",
        subfields = List(
          MarcSubfield(
            tag = "c",
            content = "1923."
          )
        )
      )
    )

    val bibData = SierraBibData(
      id = "r1000001",
      title = Some("A red rhinoceros riding a railroad"),
      varFields = varFields
    )

    transformer.getPublicationDate(bibData).get shouldBe Period("1923.")
  }

  it("ignores other non-$c subfields") {
    val varFields = List(
      VarField(
        fieldTag = "?",
        marcTag = "260",
        indicator1 = " ",
        indicator2 = " ",
        subfields = List(
          MarcSubfield(tag = "a", content = "Paris ;"),
          MarcSubfield(tag = "a", content = "New York :"),
          MarcSubfield(tag = "b", content = "Vogue,"),
          MarcSubfield(tag = "c", content = "1964-")
        )
      )
    )

    val bibData = SierraBibData(
      id = "r2000002",
      title = Some("Rude ribbets from riled reptiles"),
      varFields = varFields
    )

    transformer.getPublicationDate(bibData).get shouldBe Period("1964-")
  }

  it("concatenates values from multiple 260 fields or multiple $c subfields") {
    val varFields = List(
      VarField(
        fieldTag = "?",
        marcTag = "260",
        indicator1 = " ",
        indicator2 = " ",
        subfields = List(
          MarcSubfield(tag = "a", content = "Victoria, B. C. :"),
          MarcSubfield(tag = "c", content = "1898-1945.")
        )
      ),
      VarField(
        fieldTag = "?",
        marcTag = "261",
        indicator1 = " ",
        indicator2 = " ",
        subfields = List()
      ),
      VarField(
        fieldTag = "?",
        marcTag = "260",
        indicator1 = " ",
        indicator2 = " ",
        subfields = List(
          MarcSubfield(tag = "a", content = "[New York] :"),
          MarcSubfield(
            tag = "b",
            content = "American Statistical Association,"),
          MarcSubfield(tag = "c", content = "1975."),
          MarcSubfield(tag = "c", content = "1976.")
        )
      )
    )

    val bibData = SierraBibData(
      id = "r3000003",
      title = Some("Running riot with rovers is rarely risible"),
      varFields = varFields
    )

    transformer.getPublicationDate(bibData).get shouldBe Period(
      "1898-1945.; 1975.; 1976.")
  }

  it("returns None if there is no MARC field 260 with subfield $c") {
    val bibData = SierraBibData(
      id = "r4000004",
      title = Some("Rattling rats reach for racoons"),
      varFields = List()
    )

    transformer.getPublicationDate(bibData) shouldBe None
  }
}
