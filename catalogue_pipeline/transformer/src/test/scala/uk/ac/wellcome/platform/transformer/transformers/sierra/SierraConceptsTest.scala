package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{Concept, Identifiable, IdentifierType, SourceIdentifier}
import uk.ac.wellcome.platform.transformer.source.{MarcSubfield, VarField}

class SierraConceptsTest extends FunSpec with Matchers {

  it("extracts identifiers from subfield 0") {
    val concept = Concept(label = "Perservering puffins push past perspiration")

    val maybeIdentifiedConcept = transformer.identifyPrimaryConcept[Concept](
      concept = concept,
      varField = VarField(
        fieldTag = "p",
        marcTag = "655",
        indicator1 = "",
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
      sourceIdentifier = sourceIdentifier,
      identifiers = List(sourceIdentifier)
    )
  }

  it("deduplicates identifiers in subfield 0") {
    val concept = Concept(label = "Metaphysical mice migrating to Mars")

    val maybeIdentifiedConcept = transformer.identifyPrimaryConcept[Concept](
      concept = concept,
      varField = VarField(
        fieldTag = "p",
        marcTag = "655",
        indicator1 = "",
        indicator2 = "0",
        subfields = List(
          MarcSubfield(tag = "a", content = "martians"),
          MarcSubfield(tag = "0", content = "lcsh/bbb"),
          MarcSubfield(tag = "0", content = "lcsh/bbb")
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
      sourceIdentifier = sourceIdentifier,
      identifiers = List(sourceIdentifier)
    )
  }

  it("throws an error if it sees too many subfield 0 instances") {
    val caught = intercept[RuntimeException] {
      transformer.identifyPrimaryConcept[Concept](
        concept = Concept(label = "Hitchhiking horses hurry home"),
        varField = VarField(
          fieldTag = "p",
          marcTag = "655",
          indicator1 = "",
          indicator2 = "0",
          subfields = List(
            MarcSubfield(tag = "a", content = "hitchhiking"),
            MarcSubfield(tag = "0", content = "u/xxx"),
            MarcSubfield(tag = "0", content = "u/yyy")
          )
        )
      )
    }

    caught.getMessage shouldEqual "Too many identifiers fields: List(MarcSubfield(0,u/xxx), MarcSubfield(0,u/yyy))"
  }

  val transformer = new SierraConcepts {}
}
