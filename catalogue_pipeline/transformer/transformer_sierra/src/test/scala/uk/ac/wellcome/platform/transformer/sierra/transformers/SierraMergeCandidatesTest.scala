package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.{
  IdentifierType,
  MergeCandidate,
  SourceIdentifier
}
import uk.ac.wellcome.platform.transformer.sierra.source.{
  MarcSubfield,
  SierraBibData,
  SierraMaterialType,
  VarField
}
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

  val mergeCandidateBibNumber = "b21414440"
  val miroID = "A0123456"

  describe("physical/digital Sierra work") {
    it("extracts the bib number in 776$$w as a mergeCandidate") {
      val sierraData = createSierraBibDataWith(
        varFields = create776subfieldsWith(
          ids = List(s"(UkLW)$mergeCandidateBibNumber")
        )
      )

      transformer.getMergeCandidates(sierraData) shouldBe
        physicalAndDigitalSierraMergeCandidate(mergeCandidateBibNumber)
    }

    it("strips spaces in tag 776$$w and adds it as a mergeCandidate") {
      val sierraData = createSierraBibDataWith(
        varFields = create776subfieldsWith(
          ids = List(s"(UkLW)  $mergeCandidateBibNumber")
        )
      )

      transformer.getMergeCandidates(sierraData) shouldBe
        physicalAndDigitalSierraMergeCandidate(mergeCandidateBibNumber)
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
        varFields = create776subfieldsWith(
          ids = List("(OCoLC)14322288")
        )
      )

      transformer.getMergeCandidates(sierraData) shouldBe Nil
    }

    it(
      "does not create a merge candidate if there are multiple distinct instances of 776$$w") {
      val bibData = createSierraBibDataWith(
        varFields = create776subfieldsWith(
          ids = List(s"(UkLW)  $mergeCandidateBibNumber", "(UkLW)b12345678")
        )
      )

      transformer.getMergeCandidates(bibData) shouldBe List()
    }

    it(
      "creates a merge candidate if there are multiple 776$$w for the same value") {
      val bibData = createSierraBibDataWith(
        varFields = create776subfieldsWith(
          ids = List(
            s"(UkLW)  $mergeCandidateBibNumber",
            s"(UkLW)  $mergeCandidateBibNumber",
            s"(UkLW)$mergeCandidateBibNumber",
          )
        )
      )

      transformer.getMergeCandidates(bibData) shouldBe
        physicalAndDigitalSierraMergeCandidate(mergeCandidateBibNumber)
    }
  }

  describe("single-page Miro/Sierra work") {
    it("extracts a MIRO ID from a URL in MARC tag 962 subfield u") {
      val bibData = createMiroPictureWith(
        urls = List(s"http://wellcomeimages.org/indexplus/image/$miroID.html")
      )

      transformer.getMergeCandidates(bibData) shouldBe singleMiroMergeCandidate(
        miroID)
    }

    it(
      "does not put a merge candidate for multiple distinct instances of 962 subfield u") {
      val bibData = createMiroPictureWith(
        urls = List(
          s"http://wellcomeimages.org/indexplus/image/$miroID.html",
          "http://wellcomeimages.org/ixbin/hixclient?MIROPAC=B0000001"
        )
      )

      transformer.getMergeCandidates(bibData) shouldBe List()
    }

    it("creates a merge candidate if multiple URLs point to the same Miro ID") {
      val bibData = createMiroPictureWith(
        urls = List(
          s"http://wellcomeimages.org/indexplus/image/$miroID.html",
          s"http://wellcomeimages.org/ixbin/hixclient?MIROPAC=$miroID"
        )
      )

      transformer.getMergeCandidates(bibData) shouldBe singleMiroMergeCandidate(
        miroID)
    }

    it("does not create a merge candidate if the URL is unrecognised") {
      val bibData = createMiroPictureWith(
        urls = List(
          "http://film.wellcome.ac.uk:15151/mediaplayer.html?fug_7340-1&pw=524ph=600.html")
      )

      transformer.getMergeCandidates(bibData) shouldBe List()
    }

    it("creates a merge candidate if the material type is 'Picture'") {
      val bibData = createSierraBibDataWith(
        materialType = Some(SierraMaterialType(code = "k")),
        varFields = create962subfieldsWith(
          urls = List(s"http://wellcomeimages.org/indexplus/image/$miroID.html")
        )
      )

      transformer.getMergeCandidates(bibData) shouldBe singleMiroMergeCandidate(
        miroID)
    }

    it("creates a merge candidate if the material type is 'Digital Images'") {
      val bibData = createSierraBibDataWith(
        materialType = Some(SierraMaterialType(code = "q")),
        varFields = create962subfieldsWith(
          urls = List(s"http://wellcomeimages.org/indexplus/image/$miroID.html")
        )
      )

      transformer.getMergeCandidates(bibData) shouldBe singleMiroMergeCandidate(
        miroID)
    }

    it(
      "does not create a merge candidate if the material type is neither 'Picture' nor 'Digital Images'") {
      val bibData = createSierraBibDataWith(
        materialType = Some(SierraMaterialType(code = "x")),
        varFields = create962subfieldsWith(
          urls = List(s"http://wellcomeimages.org/indexplus/image/$miroID.html")
        )
      )

      transformer.getMergeCandidates(bibData) shouldBe List()
    }
  }

  it(
    "creates merge candidates for both physical/digital Sierra works and Miro works") {
    val varFields =
      create776subfieldsWith(ids = List(s"(UkLW)$mergeCandidateBibNumber")) ++
        create962subfieldsWith(urls =
          List(s"http://wellcomeimages.org/indexplus/image/$miroID.html"))

    val sierraData = createSierraBibDataWith(
      materialType = Some(SierraMaterialType("k")),
      varFields = varFields
    )

    val expectedMergeCandidates =
      physicalAndDigitalSierraMergeCandidate(mergeCandidateBibNumber) ++
        singleMiroMergeCandidate(miroID)

    transformer.getMergeCandidates(sierraData) shouldBe expectedMergeCandidates
  }

  it("returns an empty list if there is no MARC tag 776 or 962") {
    val sierraData = createSierraBibDataWith(varFields = List())
    transformer.getMergeCandidates(sierraData) shouldBe Nil
  }

  private def physicalAndDigitalSierraMergeCandidate(
    bibNumber: String): List[MergeCandidate] =
    List(
      MergeCandidate(
        identifier = SourceIdentifier(
          identifierType = IdentifierType("sierra-system-number"),
          ontologyType = "Work",
          value = bibNumber
        ),
        reason = Some("Physical/digitised Sierra work")
      )
    )

  private def singleMiroMergeCandidate(miroID: String): List[MergeCandidate] =
    List(
      MergeCandidate(
        identifier = SourceIdentifier(
          identifierType = IdentifierType("miro-image-number"),
          ontologyType = "Work",
          value = miroID
        ),
        reason = Some("Single page Miro/Sierra work")
      )
    )

  private def create776subfieldsWith(ids: List[String]): List[VarField] =
    ids.map { idString =>
      createVarFieldWith(
        marcTag = "776",
        subfields = List(
          MarcSubfield(tag = "w", content = idString)
        )
      )
    }

  private def createMiroPictureWith(urls: List[String]): SierraBibData =
    createSierraBibDataWith(
      materialType = Some(SierraMaterialType(code = "k")),
      varFields = create962subfieldsWith(urls = urls)
    )

  private def create962subfieldsWith(urls: List[String]): List[VarField] =
    urls.map { url =>
      createVarFieldWith(
        marcTag = "962",
        subfields = List(
          MarcSubfield(tag = "u", content = url)
        )
      )
    }
}
