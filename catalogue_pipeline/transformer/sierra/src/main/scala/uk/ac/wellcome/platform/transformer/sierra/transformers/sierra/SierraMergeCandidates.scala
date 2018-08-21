package uk.ac.wellcome.platform.transformer.sierra.transformers.sierra

import uk.ac.wellcome.models.work.internal.{
  IdentifierType,
  MergeCandidate,
  SourceIdentifier
}

trait SierraMergeCandidates extends MarcUtils {

  // Populate mergeCandidates from tag 776 subfield w if starting with (UkLW), ignore otherwise.
  // Strip the (UkLW) prefix and return the rest as a bibNumber of a merge candidate
  def getMergeCandidates(sierraBibData: SierraBibData): List[MergeCandidate] =
    for {
      subFields <- getMatchingSubfields(sierraBibData, "776", "w")
      subField <- subFields
      mergeCandidate <- extractMergeCandidate(subField.content).toList
    } yield mergeCandidate

  private def extractMergeCandidate(content: String): Option[MergeCandidate] = {
    // This regex matches any string starting with (UkLW), followed by any number of spaces
    // and groups everything after that, which is the sierra bib number we're interested in
    val prefixRegex = """\(UkLW\)[\s]*(.+)""".r.anchored

    content match {
      case prefixRegex(bibNumber) =>
        Some(
          MergeCandidate(
            SourceIdentifier(
              IdentifierType("sierra-system-number"),
              "Work",
              bibNumber)))
      case _ => None
    }
  }
}
