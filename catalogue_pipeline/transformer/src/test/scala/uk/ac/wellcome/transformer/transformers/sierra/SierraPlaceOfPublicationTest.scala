package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.Place
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData, VarField}

class SierraPlaceOfPublicationTest extends FunSpec with Matchers {

  val transformer = new SierraPlaceOfPublication {}

  it(
    "creates a place of publication with no identifier from field 260 subfield a") {
    val content = "Somewhere over the rainbow"
    val bibData = SierraBibData(
      id = "1234567",
      title = None,
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "260",
          indicator1 = "",
          indicator2 = "",
          subfields = List(MarcSubfield(tag = "a", content = content))))
    )

    val bibWithPlaceOfPublication = transformer.getPlacesOfPublication(bibData)

    bibWithPlaceOfPublication shouldBe List(Place(label = content))
  }

  it("ignores subfields other than a") {
    val content = "Somewhere over the rainbow"
    val bibData = SierraBibData(
      id = "1234567",
      title = None,
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "260",
          indicator1 = "",
          indicator2 = "",
          subfields = List(MarcSubfield(tag = "b", content = content))))
    )

    val bibWithPlaceOfPublication = transformer.getPlacesOfPublication(bibData)

    bibWithPlaceOfPublication shouldBe Nil
  }

}
