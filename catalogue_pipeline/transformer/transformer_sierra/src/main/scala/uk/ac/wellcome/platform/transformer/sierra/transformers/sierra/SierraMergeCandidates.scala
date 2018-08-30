package uk.ac.wellcome.platform.transformer.sierra.transformers.sierra

import uk.ac.wellcome.models.work.internal.{
  IdentifierType,
  MergeCandidate,
  SourceIdentifier
}
import uk.ac.wellcome.platform.transformer.sierra.source.{MarcSubfield, SierraBibData}

import scala.util.matching.Regex

trait SierraMergeCandidates extends MarcUtils {

  def getMergeCandidates(sierraBibData: SierraBibData): List[MergeCandidate] =
    get776mergeCandidates(sierraBibData)

  // This regex matches any string starting with (UkLW), followed by
  // any number of spaces, and then captures everything after the
  // space, which is the bib number we're interested in.
  private val uklwPrefixRegex: Regex = """\(UkLW\)[\s]*(.+)""".r.anchored

  /** We can merge a bib and the digitised version of that bib.  The number
    * of the other bib comes from MARC tag 776 subfield $w.
    *
    * If the identifier starts with (UkLW), we strip the prefix and use the
    * bib number as a merge candidate.
    *
    */
  private def get776mergeCandidates(sierraBibData: SierraBibData): List[MergeCandidate] = {
    val matchingSubfields: List[MarcSubfield] = getMatchingSubfields(
      sierraBibData,
      marcTag = "776",
      marcSubfieldTag = "w"
    ).flatten

    matchingSubfields match {
      case List(MarcSubfield(_, uklwPrefixRegex(bibNumber))) =>
        List(
          MergeCandidate(
            SourceIdentifier(
              identifierType = IdentifierType("sierra-system-number"),
              ontologyType = "Work",
              value = bibNumber
            )
          )
        )
      case _ => List()
    }
  }
}
