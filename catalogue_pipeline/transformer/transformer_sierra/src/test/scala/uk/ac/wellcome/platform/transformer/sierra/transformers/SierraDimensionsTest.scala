package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.transformer.sierra.source.MarcSubfield
import uk.ac.wellcome.platform.transformer.sierra.generators.{
  MarcGenerators,
  SierraDataGenerators
}

class SierraDimensionsTest
    extends FunSpec
    with Matchers
    with MarcGenerators
    with SierraDataGenerators {

  val transformer = new SierraDimensions {}

  it("gets no dimensions if there is no MARC field 300 with subfield $$c") {
    val bibData = createSierraBibDataWith(varFields = List())
    transformer.getDimensions(bibData = bibData) shouldBe None
  }

  it("extracts dimensions from MARC field 300 subfield $$c") {
    val dimensions = "23cm"

    val varFields = List(
      createVarFieldWith(
        marcTag = "300",
        subfields = List(
          MarcSubfield(tag = "a", content = "149 p. ;"),
          MarcSubfield(tag = "c", content = "23cm")
        )
      )
    )

    val bibData = createSierraBibDataWith(varFields = varFields)
    transformer.getDimensions(bibData = bibData) shouldBe Some(dimensions)
  }

  it("extracts an dimensions where there are multiple MARC field 300 $$c") {
    val dimensions1 = "21 cm. +"
    val dimensions2 = "37cm"

    val expectedDimensions = s"$dimensions1 $dimensions2"

    val varFields = List(
      createVarFieldWith(
        marcTag = "300",
        subfields = List(
          MarcSubfield(tag = "a", content = "1 print :"),
          MarcSubfield(tag = "c", content = dimensions1)
        )
      ),
      createVarFieldWith(
        marcTag = "300",
        subfields = List(
          MarcSubfield(tag = "c", content = dimensions2)
        )
      )
    )

    val bibData = createSierraBibDataWith(varFields = varFields)
    transformer.getDimensions(bibData = bibData) shouldBe Some(
      expectedDimensions)
  }
}
