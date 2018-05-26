package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.source.{MarcSubfield, VarField}

class SierraConceptIdentifierTest extends FunSpec with Matchers {

  it("finds an LCSH identifier") {
    val varField = baseVarField.copy(indicator2 = Some("0"))
    val identifierSubfield = MarcSubfield(tag = "0", content = "lcsh/123")

    val expectedSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("LCSH"),
      value = "lcsh/123",
      ontologyType = ontologyType
    )

    val actualSourceIdentifier = SierraConceptIdentifier
      .maybeFindIdentifier(
        varField = varField,
        identifierSubfield = identifierSubfield,
        ontologyType = ontologyType
      )
      .get

    actualSourceIdentifier shouldBe expectedSourceIdentifier
  }

  it("finds a MESH identifier") {
    val varField = baseVarField.copy(indicator2 = Some("2"))
    val identifierSubfield = MarcSubfield(tag = "0", content = "mesh/456")

    val expectedSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("MESHId"),
      value = "mesh/456",
      ontologyType = ontologyType
    )

    val actualSourceIdentifier = SierraConceptIdentifier
      .maybeFindIdentifier(
        varField = varField,
        identifierSubfield = identifierSubfield,
        ontologyType = ontologyType
      )
      .get

    actualSourceIdentifier shouldBe expectedSourceIdentifier
  }

  it("finds a no-ID identifier if indicator 2 = 4") {
    val varField = baseVarField.copy(indicator2 = Some("4"))
    val identifierSubfield = MarcSubfield(tag = "0", content = "noid/000")

    SierraConceptIdentifier.maybeFindIdentifier(
      varField = varField,
      identifierSubfield = identifierSubfield,
      ontologyType = ontologyType
    ) shouldBe None
  }

  it("returns None if indicator 2 is empty") {
    val varField = baseVarField.copy(indicator2 = None)
    val identifierSubfield = MarcSubfield(tag = "0", content = "lcsh/789")

    SierraConceptIdentifier.maybeFindIdentifier(
      varField = varField,
      identifierSubfield = identifierSubfield,
      ontologyType = ontologyType
    ) shouldBe None
  }

  it("returns None if it sees an unrecognised identifier scheme") {
    val identifierSubfield = MarcSubfield(tag = "0", content = "u/xxx")
    val varField = baseVarField.copy(
      indicator2 = Some("8"),
      subfields = List(identifierSubfield)
    )

    SierraConceptIdentifier.maybeFindIdentifier(
      varField = varField,
      identifierSubfield = identifierSubfield,
      ontologyType = ontologyType
    ) shouldBe None
  }

  it("passes through the ontology type") {
    val varField = baseVarField.copy(indicator2 = Some("2"))
    val identifierSubfield = MarcSubfield(tag = "0", content = "mesh/456")

    val expectedSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("MESHId"),
      value = "mesh/456",
      ontologyType = "Item"
    )

    val actualSourceIdentifier = SierraConceptIdentifier
      .maybeFindIdentifier(
        varField = varField,
        identifierSubfield = identifierSubfield,
        ontologyType = "Item"
      )
      .get

    actualSourceIdentifier shouldBe expectedSourceIdentifier
  }

  val ontologyType = "Concept"

  val baseVarField = VarField(
    fieldTag = "p",
    marcTag = "655",
    indicator1 = "",
    indicator2 = "",
    subfields = List()
  )
}
