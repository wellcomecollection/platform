package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier}
import uk.ac.wellcome.platform.transformer.sierra.generators.{
  MarcGenerators,
  SierraDataGenerators
}
import uk.ac.wellcome.platform.transformer.sierra.source.MarcSubfield

class SierraIdentifiersTest
    extends FunSpec
    with Matchers
    with MarcGenerators
    with SierraDataGenerators {

  it("passes through the main identifier from the bib record") {
    val bibId = createSierraBibNumber

    val expectedIdentifiers = List(
      SourceIdentifier(
        identifierType = IdentifierType("sierra-identifier"),
        ontologyType = "Work",
        value = bibId.withoutCheckDigit
      )
    )

    val otherIdentifiers =
      transformer.getOtherIdentifiers(bibId, bibData = createSierraBibData)

    otherIdentifiers shouldBe expectedIdentifiers
  }

  it("passes through an ISBN identifier if present") {
    val isbn = "1785783033"

    val bibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "020",
          subfields = List(
            MarcSubfield(tag = "a", content = isbn)
          )
        )
      )
    )

    val otherIdentifiers = transformer.getOtherIdentifiers(
      bibId = createSierraBibNumber,
      bibData = bibData
    )

    otherIdentifiers should contain(
      SourceIdentifier(
        identifierType = IdentifierType("isbn"),
        ontologyType = "Work",
        value = isbn
      ))
  }

  it("passes through multiple ISBN identifiers if present") {
    val isbn10 = "1473647649"
    val isbn13 = "978-1473647640"

    val bibData = createSierraBibDataWith(
      varFields = List(
        createVarFieldWith(
          marcTag = "020",
          subfields = List(
            MarcSubfield(tag = "a", content = isbn10)
          )
        ),
        createVarFieldWith(
          marcTag = "020",
          subfields = List(
            MarcSubfield(tag = "a", content = isbn13)
          )
        )
      )
    )

    val otherIdentifiers = transformer.getOtherIdentifiers(
      bibId = createSierraBibNumber,
      bibData = bibData
    )

    otherIdentifiers should contain(
      SourceIdentifier(
        identifierType = IdentifierType("isbn"),
        ontologyType = "Work",
        value = isbn10
      ))

    otherIdentifiers should contain(
      SourceIdentifier(
        identifierType = IdentifierType("isbn"),
        ontologyType = "Work",
        value = isbn13
      ))
  }

  val transformer = new Object with SierraIdentifiers
}
