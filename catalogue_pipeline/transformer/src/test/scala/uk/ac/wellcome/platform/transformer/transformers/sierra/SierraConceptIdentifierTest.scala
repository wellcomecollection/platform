package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.source.VarField

class SierraConceptIdentifierTest extends FunSpec with Matchers {

  it("finds an LCSH identifier") {
    val varField = baseVarField.copy(indicator2 = Some("0"))

    val expectedSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("lc-subjects"),
      value = "lcsh/123",
      ontologyType = ontologyType
    )

    val actualSourceIdentifier = SierraConceptIdentifier
      .maybeFindIdentifier(
        varField = varField,
        identifierSubfieldContent = "lcsh/123",
        ontologyType = ontologyType
      )
      .get

    actualSourceIdentifier shouldBe expectedSourceIdentifier
  }

  it("finds a MESH identifier") {
    val varField = baseVarField.copy(indicator2 = Some("2"))

    val expectedSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("nlm-mesh"),
      value = "mesh/456",
      ontologyType = ontologyType
    )

    val actualSourceIdentifier = SierraConceptIdentifier
      .maybeFindIdentifier(
        varField = varField,
        identifierSubfieldContent = "mesh/456",
        ontologyType = ontologyType
      )
      .get

    actualSourceIdentifier shouldBe expectedSourceIdentifier
  }

  it("finds a no-ID identifier if indicator 2 = 4") {
    val varField = baseVarField.copy(indicator2 = Some("4"))

    SierraConceptIdentifier.maybeFindIdentifier(
      varField = varField,
      identifierSubfieldContent = "noid/000",
      ontologyType = ontologyType
    ) shouldBe None
  }

  it("returns None if indicator 2 is empty") {
    val varField = baseVarField.copy(indicator2 = None)

    SierraConceptIdentifier.maybeFindIdentifier(
      varField = varField,
      identifierSubfieldContent = "lcsh/789",
      ontologyType = ontologyType
    ) shouldBe None
  }

  it("returns None if it sees an unrecognised identifier scheme") {
    val varField = baseVarField.copy(indicator2 = Some("8"))

    SierraConceptIdentifier.maybeFindIdentifier(
      varField = varField,
      identifierSubfieldContent = "u/xxx",
      ontologyType = ontologyType
    ) shouldBe None
  }

  it("passes through the ontology type") {
    val varField = baseVarField.copy(indicator2 = Some("2"))

    val expectedSourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("nlm-mesh"),
      value = "mesh/456",
      ontologyType = "Item"
    )

    val actualSourceIdentifier = SierraConceptIdentifier
      .maybeFindIdentifier(
        varField = varField,
        identifierSubfieldContent = "mesh/456",
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
