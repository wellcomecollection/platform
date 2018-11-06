package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.transformer.sierra.source.MarcSubfield
import uk.ac.wellcome.platform.transformer.sierra.generators.{
  MarcGenerators,
  SierraDataGenerators
}

class SierraExtentTest
    extends FunSpec
    with Matchers
    with MarcGenerators
    with SierraDataGenerators {

  val transformer = new SierraExtent {}

  it("gets no extent if there is no MARC field 300 with subfield $$a") {
    val bibData = createSierraBibDataWith(varFields = List())
    transformer.getExtent(bibData = bibData) shouldBe None
  }

  it("extracts extent from MARC field 300 subfield $$a") {
    val extent = "Eleven elephant etchings"

    val varFields = List(
      createVarFieldWith(
        marcTag = "300",
        subfields = List(
          MarcSubfield(tag = "a", content = extent),
          MarcSubfield(tag = "b", content = "Grey, gigantic, graceful (?)")
        )
      )
    )

    val bibData = createSierraBibDataWith(varFields = varFields)
    transformer.getExtent(bibData = bibData) shouldBe Some(extent)
  }

  it("extracts an extent where there are multiple MARC field 300 $$a") {
    // This is based on the "repeatable $a" example in the MARC spec.
    // https://www.loc.gov/marc/bibliographic/bd300.html
    val extent1 = "diary"
    val extent2 = "1"
    val extent3 = "(463"

    val expectedExtent = s"$extent1 $extent2 $extent3"

    val varFields = List(
      createVarFieldWith(
        marcTag = "300",
        subfields = List(
          MarcSubfield(tag = "a", content = extent1)
        )
      ),
      createVarFieldWith(
        marcTag = "300",
        subfields = List(
          MarcSubfield(tag = "a", content = extent2),
          MarcSubfield(
            tag = "b",
            content = "Endless ecstasy from ecclesiastic echoes")
        )
      ),
      createVarFieldWith(
        marcTag = "300",
        subfields = List(
          MarcSubfield(tag = "a", content = extent3)
        )
      )
    )

    val bibData = createSierraBibDataWith(varFields = varFields)
    transformer.getExtent(bibData = bibData) shouldBe Some(expectedExtent)
  }
}
