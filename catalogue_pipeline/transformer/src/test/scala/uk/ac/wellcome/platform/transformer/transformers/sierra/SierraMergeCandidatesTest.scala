package uk.ac.wellcome.platform.transformer.transformers.sierra

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraData
import uk.ac.wellcome.models.work.internal.{IdentifierType, MergeCandidate, SourceIdentifier}
import uk.ac.wellcome.platform.transformer.source.{MarcSubfield, SierraBibData, VarField}

class SierraMergeCandidatesTest extends FunSpec with Matchers with SierraData {

  val transformer = new SierraMergeCandidates{}
  it("extracts the bib number in 776$$w and adds it as a mergeCandidate") {
    val mergeCandidateBibNumber = "b21414440"
    val sierraData = SierraBibData(
      id = "b1234567",
      title = Some("A pack of published puffins in Paris"),
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "776",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "w", content = s"(UkLW)$mergeCandidateBibNumber")
          )
        )
      )
    )

    transformer.getMergeCandidates(sierraData) shouldBe List(MergeCandidate(SourceIdentifier(IdentifierType("sierra-system-number"), "Work", mergeCandidateBibNumber)))
  }

  it("strips spaces in tag 776$$w and adds it as a mergeCandidate") {
    val mergeCandidateBibNumber = "b21414440"
    val sierraData = SierraBibData(
      id = "b1234567",
      title = Some("A pack of published puffins in Paris"),
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "776",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "w", content = s"(UkLW)  $mergeCandidateBibNumber")
          )
        )
      )
    )

    transformer.getMergeCandidates(sierraData) shouldBe List(MergeCandidate(SourceIdentifier(IdentifierType("sierra-system-number"), "Work", mergeCandidateBibNumber)))
  }

  it("returns an empty list if there is no marc tag 776") {
    val sierraData = SierraBibData(
      id = "b1234567",
      title = Some("A pack of published puffins in Paris"),
      varFields = List( )
    )

    transformer.getMergeCandidates(sierraData) shouldBe Nil
  }

  it("returns an empty list if marc tag 776 does not contain a subfield w") {
    val sierraData = SierraBibData(
      id = "b1234567",
      title = Some("A pack of published puffins in Paris"),
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "776",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "a", content = s"blah blah")
          )
        )
      )
    )

    transformer.getMergeCandidates(sierraData) shouldBe Nil
  }

  it("ignores values in 776$$w that aren't prefixed with (UkLW)") {
    val sierraData = SierraBibData(
      id = "b1234567",
      title = Some("A pack of published puffins in Paris"),
      varFields = List(
        VarField(
          fieldTag = "p",
          marcTag = "776",
          indicator1 = "",
          indicator2 = "",
          subfields = List(
            MarcSubfield(tag = "w", content = s"(OCoLC)14322288")
          )
        )
      )
    )

    transformer.getMergeCandidates(sierraData) shouldBe Nil
  }
}

trait SierraMergeCandidates extends MarcUtils{

  def getMergeCandidates(sierraBibData: SierraBibData): List[MergeCandidate] = for {
    subFields <- getMatchingSubfields(sierraBibData, "776", "w")
    subField <- subFields
    mergeCandidate <- extractMergeCandidate(subField.content).toList
  } yield mergeCandidate

  private def extractMergeCandidate(content: String): Option[MergeCandidate] = {
    // This regex matches any string starting with (UkLW), followed by any number of spaces
    // and groups everything after that, which is the sierra bib number we're interested in
    val prefixRegex = """\(UkLW\)[\s]*(.+)""".r.anchored

    content match {
      case prefixRegex(bibNumber) => Some(MergeCandidate(SourceIdentifier(IdentifierType("sierra-system-number"), "Work", bibNumber)))
      case _ => None
    }
  }
}
