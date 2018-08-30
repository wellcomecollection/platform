package uk.ac.wellcome.platform.transformer.sierra.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{
  IdentifierType,
  MergeCandidate,
  SourceIdentifier
}
import uk.ac.wellcome.platform.transformer.sierra.source.MarcSubfield
import uk.ac.wellcome.platform.transformer.sierra.generators.{
  MarcGenerators,
  SierraDataGenerators
}

class SierraMergeCandidatesTest
    extends FunSpec
    with Matchers
    with MarcGenerators
    with SierraDataGenerators {

  val transformer = new SierraMergeCandidates {}

  describe("physical/digital Sierra work") {
    it("extracts the bib number in 776$$w as a mergeCandidate") {
      val mergeCandidateBibNumber = "b21414440"
      val sierraData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(
            marcTag = "776",
            subfields = List(
              MarcSubfield(tag = "w", content = s"(UkLW)$mergeCandidateBibNumber")
            )
          )
        )
      )

      transformer.getMergeCandidates(sierraData) shouldBe List(
        MergeCandidate(
          SourceIdentifier(
            IdentifierType("sierra-system-number"),
            "Work",
            mergeCandidateBibNumber)))
    }

    it("strips spaces in tag 776$$w and adds it as a mergeCandidate") {
      val mergeCandidateBibNumber = "b21414440"
      val sierraData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(
            marcTag = "776",
            subfields = List(
              MarcSubfield(
                tag = "w",
                content = s"(UkLW)  $mergeCandidateBibNumber")
            )
          )
        )
      )

      transformer.getMergeCandidates(sierraData) shouldBe List(
        MergeCandidate(
          SourceIdentifier(
            IdentifierType("sierra-system-number"),
            "Work",
            mergeCandidateBibNumber)))
    }

    it("returns an empty list if MARC tag 776 does not contain a subfield w") {
      val sierraData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(
            marcTag = "776",
            subfields = List(
              MarcSubfield(tag = "a", content = s"blah blah")
            )
          )
        )
      )

      transformer.getMergeCandidates(sierraData) shouldBe Nil
    }

    it("ignores values in 776$$w that aren't prefixed with (UkLW)") {
      val sierraData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(
            marcTag = "776",
            subfields = List(
              MarcSubfield(tag = "w", content = s"(OCoLC)14322288")
            )
          )
        )
      )

      transformer.getMergeCandidates(sierraData) shouldBe Nil
    }
  }

  describe("single-page Miro/Sierra work") {
    it("extracts a MIRO ID from a URL in MARC tag 962 subfield u") {
      val miroID = "A0123456"
      val bibData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(
            marcTag = "962",
            subfields = List(
              MarcSubfield(
                tag = "u",
                content = s"http://wellcomeimages.org/indexplus/image/$miroID.html"
              )
            )
          )
        )
      )

      transformer.getMergeCandidates(bibData) shouldBe List(
        MergeCandidate(
          identifier = SourceIdentifier(
            identifierType = IdentifierType("miro-image-number"),
            ontologyType = "Work",
            value = miroID
          )
        )
      )
    }
  }

  it("returns an empty list if there is no MARC tag 776") {
    val sierraData = createSierraBibDataWith(varFields = List())
    transformer.getMergeCandidates(sierraData) shouldBe Nil
  }
}
