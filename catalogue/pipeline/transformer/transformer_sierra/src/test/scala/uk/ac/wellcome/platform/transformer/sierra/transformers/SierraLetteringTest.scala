package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.transformer.sierra.source.{
  MarcSubfield,
  VarField
}
import uk.ac.wellcome.platform.transformer.sierra.generators.{
  MarcGenerators,
  SierraDataGenerators
}

class SierraLetteringTest
    extends FunSpec
    with Matchers
    with MarcGenerators
    with SierraDataGenerators {

  it("ignores records with the wrong MARC field") {
    assertFindsCorrectLettering(
      varFields = List(
        createVarFieldWith(
          marcTag = "300",
          indicator2 = "6",
          subfields = List(
            MarcSubfield(tag = "a", content = "Alas, ailments are annoying")
          )
        )
      ),
      expectedLettering = None
    )
  }

  it("ignores records with the right MARC field but wrong indicator 2") {
    assertFindsCorrectLettering(
      varFields = List(
        createVarFieldWith(
          marcTag = "246",
          indicator2 = "7",
          subfields = List(
            MarcSubfield(tag = "a", content = "Alas, ailments are annoying")
          )
        )
      ),
      expectedLettering = None
    )
  }

  it("ignores records with the MARC field and 2nd indicator but wrong subfield") {
    assertFindsCorrectLettering(
      varFields = List(
        createVarFieldWith(
          marcTag = "246",
          indicator2 = "6",
          subfields = List(
            MarcSubfield(
              tag = "b",
              content = "Belligerent beavers beneath a bridge")
          )
        )
      ),
      expectedLettering = None
    )
  }

  it("passes through a single instance of 246 .6 $$a, if present") {
    assertFindsCorrectLettering(
      varFields = List(
        createVarFieldWith(
          marcTag = "246",
          indicator2 = "6",
          subfields = List(
            MarcSubfield(
              tag = "a",
              content = "Crowded crows carry a chocolate crepe")
          )
        )
      ),
      expectedLettering = Some("Crowded crows carry a chocolate crepe")
    )
  }

  it("joins multiple instances of 246 .6 $$a, if present") {
    assertFindsCorrectLettering(
      varFields = List(
        createVarFieldWith(
          marcTag = "246",
          indicator2 = "6",
          subfields = List(
            MarcSubfield(
              tag = "a",
              content = "Daring dalmations dance with danger")
          )
        ),
        createVarFieldWith(
          marcTag = "246",
          indicator2 = "6",
          subfields = List(
            MarcSubfield(
              tag = "a",
              content = "Enterprising eskimos exile every eagle")
          )
        )
      ),
      expectedLettering = Some(
        "Daring dalmations dance with danger\n\nEnterprising eskimos exile every eagle")
    )
  }

  val transformer = new SierraLettering {}

  private def assertFindsCorrectLettering(
    varFields: List[VarField],
    expectedLettering: Option[String]
  ) = {
    val bibData = createSierraBibDataWith(varFields = varFields)
    transformer.getLettering(bibData = bibData) shouldBe expectedLettering
  }
}
