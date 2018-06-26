package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal.{IdentifierType, MergeCandidate, SourceIdentifier}
import uk.ac.wellcome.platform.transformer.source.SierraBibData

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
