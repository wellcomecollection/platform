package uk.ac.wellcome.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models._
import uk.ac.wellcome.transformer.source.{Country, MarcSubfield, SierraBibData, VarField}

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

    val bibWithPlaceOfPublication = transformer.getPlaceOfPublication(bibData)

    bibWithPlaceOfPublication shouldBe List(
      UnidentifiablePlaceOfPublication(label = content))
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

    val bibWithPlaceOfPublication = transformer.getPlaceOfPublication(bibData)

    bibWithPlaceOfPublication shouldBe Nil
  }

  it("gets a place of publication with an identifier from the country field") {
    val bibData = SierraBibData(
      id = "1234567",
      title = None,
      country = Some(Country(code = "nyu", name = "New York"))
    )

    val bibWithPlaceOfPublication = transformer.getPlaceOfPublication(bibData)

    bibWithPlaceOfPublication shouldBe List(
      UnidentifiedPlaceOfPublication(
        label = "New York",
        sourceIdentifier =
          SourceIdentifier(IdentifierSchemes.marcCountries, "nyu"),
        identifiers =
          List(SourceIdentifier(IdentifierSchemes.marcCountries, "nyu"))
      ))
  }

}
