package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.generators.MarcGenerators
import uk.ac.wellcome.platform.transformer.sierra.source.MarcSubfield

class SierraConceptsTest extends FunSpec with Matchers with MarcGenerators {

  it("extracts identifiers from subfield 0") {
    val concept =
      Concept(label = "Perservering puffins push past perspiration")

    val maybeIdentifiedConcept = transformer.identifyConcept[Concept](
      concept = concept,
      varField = createVarFieldWith(
        marcTag = "CCC",
        indicator2 = "0",
        subfields = List(
          MarcSubfield(tag = "a", content = "pilots"),
          MarcSubfield(tag = "0", content = "lcsh/ppp")
        )
      )
    )

    val sourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("lc-subjects"),
      value = "lcsh/ppp",
      ontologyType = "Concept"
    )

    maybeIdentifiedConcept shouldBe Identifiable(
      concept,
      sourceIdentifier = sourceIdentifier
    )
  }

  it("normalises and deduplicates identifiers in subfield 0") {
    val concept = Concept(label = "Metaphysical mice migrating to Mars")

    val maybeIdentifiedConcept = transformer.identifyConcept[Concept](
      concept = concept,
      varField = createVarFieldWith(
        marcTag = "CCC",
        indicator2 = "0",
        subfields = List(
          MarcSubfield(tag = "a", content = "martians"),
          MarcSubfield(tag = "0", content = "lcsh/bbb"),
          MarcSubfield(tag = "0", content = "lcsh/bbb"),
          // Including the (DNLM) prefix
          MarcSubfield(tag = "0", content = "(DNLM)lcsh/bbb"),
          // With trailing punctuation
          MarcSubfield(tag = "0", content = "lcsh/bbb."),
          // Including whitespace
          MarcSubfield(tag = "0", content = "lcsh / bbb"),
          // Including a MESH URL prefix
          MarcSubfield(
            tag = "0",
            content = "https://id.nlm.nih.gov/mesh/lcsh/bbb")
        )
      )
    )

    val sourceIdentifier = SourceIdentifier(
      identifierType = IdentifierType("lc-subjects"),
      value = "lcsh/bbb",
      ontologyType = "Concept"
    )

    maybeIdentifiedConcept shouldBe Identifiable(
      concept,
      sourceIdentifier = sourceIdentifier
    )
  }

  it("ignores multiple instances of subfield 0 in the otherIdentifiers") {
    val concept = Concept(label = "Hitchhiking horses hurry home")

    val maybeIdentifiedConcept = transformer.identifyConcept[Concept](
      concept = concept,
      varField = createVarFieldWith(
        marcTag = "CCC",
        subfields = List(
          MarcSubfield(tag = "a", content = "hitchhiking"),
          MarcSubfield(tag = "0", content = "u/xxx"),
          MarcSubfield(tag = "0", content = "u/yyy")
        )
      )
    )

    maybeIdentifiedConcept shouldBe Unidentifiable(
      concept
    )
  }

  val transformer = new SierraConcepts {}
}
