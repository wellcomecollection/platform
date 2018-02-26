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

  it("extracts publication date from MARC field 260 with subfield c") {
    val
      varFields = List(
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
      id = "b1234567",
      title = Some("A xylophone full of xenon."),
      varFields = varFields
    )

    transformer.getPublicationDate(bibData) shouldBe Some(Period("1923."))
  }

}
