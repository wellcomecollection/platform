package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.{Agent, Organisation}
import uk.ac.wellcome.transformer.source.{
  MarcSubfield,
  SierraBibData,
  VarField
}
import uk.ac.wellcome.test.utils.SierraData

class SierraLetteringTest extends FunSpec with Matchers with SierraData {

  it("ignores records with the wrong MARC field") {
    assertFindsCorrectLettering(
      lettering = Some("An ambiance of accordions"),
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "260",
          indicator1 = " ",
          indicator2 = " ",
          subfields = List()
        )
      ),
      expectedLettering = None
    )
  }

  it("ignores records with the right MARC field but wrong indicator 1") {
    assertFindsCorrectLettering(
      lettering = Some("Bellowing bassoons in the basement"),
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "246",
          indicator1 = "2",
          indicator2 = "6",
          subfields = List()
        )
      ),
      expectedLettering = None
    )
  }

  it("ignores records with the right MARC field but wrong indicator 2") {
    assertFindsCorrectLettering(
      lettering = Some("Cantering clarinets for a concert"),
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "246",
          indicator1 = "1",
          indicator2 = "7",
          subfields = List()
        )
      ),
      expectedLettering = None
    )
  }

  it("picks up lettering if the correct MARC field is present") {
    assertFindsCorrectLettering(
      lettering = Some("Dancing to a drum"),
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "246",
          indicator1 = "1",
          indicator2 = "6",
          subfields = List()
        )
      ),
      expectedLettering = Some("Dancing to a drum")
    )
  }

  it(
    "passes through nothing if the lettering is missing, even if the MARC is correct") {
    assertFindsCorrectLettering(
      lettering = None,
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "246",
          indicator1 = "1",
          indicator2 = "6",
          subfields = List()
        )
      ),
      expectedLettering = None
    )
  }

  val transformer = new Object with SierraLettering

  private def assertFindsCorrectLettering(
    lettering: Option[String],
    varFields: List[VarField],
    expectedLettering: Option[String]
  ) = {

    val bibData = SierraBibData(
      id = "b1234567",
      title = Some("A libel of lions"),
      lettering = lettering,
      varFields = varFields
    )

    transformer.getLettering(bibData = bibData) shouldBe expectedLettering
  }
}
