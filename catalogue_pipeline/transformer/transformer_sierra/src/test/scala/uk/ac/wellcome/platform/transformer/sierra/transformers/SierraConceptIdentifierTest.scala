package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.generators.MarcGenerators
import uk.ac.wellcome.platform.transformer.sierra.source.VarField

class SierraConceptIdentifierTest
    extends FunSpec
    with Matchers
    with MarcGenerators {

  it("finds an LCSH identifier") {
    val varField = create655VarFieldWith(indicator2 = "0")

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
    val varField = create655VarFieldWith(indicator2 = "2")

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
    val varField = create655VarFieldWith(indicator2 = "4")

    SierraConceptIdentifier.maybeFindIdentifier(
      varField = varField,
      identifierSubfieldContent = "noid/000",
      ontologyType = ontologyType
    ) shouldBe None
  }

  it("returns None if indicator 2 is empty") {
    val varField = create655VarFieldWith(indicator2 = None)

    SierraConceptIdentifier.maybeFindIdentifier(
      varField = varField,
      identifierSubfieldContent = "lcsh/789",
      ontologyType = ontologyType
    ) shouldBe None
  }

  it("returns None if it sees an unrecognised identifier scheme") {
    val varField = create655VarFieldWith(indicator2 = "8")

    SierraConceptIdentifier.maybeFindIdentifier(
      varField = varField,
      identifierSubfieldContent = "u/xxx",
      ontologyType = ontologyType
    ) shouldBe None
  }

  it("passes through the ontology type") {
    val varField = create655VarFieldWith(indicator2 = "2")

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

  private def create655VarFieldWith(indicator2: Option[String]): VarField =
    createVarFieldWith(marcTag = "655", indicator2 = indicator2)

  private def create655VarFieldWith(indicator2: String): VarField =
    create655VarFieldWith(indicator2 = Some(indicator2))
}
