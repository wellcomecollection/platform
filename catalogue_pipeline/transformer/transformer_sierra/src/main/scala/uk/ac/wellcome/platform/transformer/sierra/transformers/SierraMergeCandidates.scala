package uk.ac.wellcome.platform.transformer.sierra.transformers

import uk.ac.wellcome.models.work.internal.{
  IdentifierType,
  MergeCandidate,
  SourceIdentifier
}
import uk.ac.wellcome.platform.transformer.sierra.source.{
  MarcSubfield,
  SierraBibData,
  SierraMaterialType
}

import scala.util.matching.Regex

trait SierraMergeCandidates extends MarcUtils with WellcomeImagesURLParser {

  def getMergeCandidates(sierraBibData: SierraBibData): List[MergeCandidate] =
    get776mergeCandidates(sierraBibData) ++
      getSinglePageMiroMergeCandidates(sierraBibData)

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
  private def get776mergeCandidates(
    sierraBibData: SierraBibData): List[MergeCandidate] = {
    val matchingSubfields: List[MarcSubfield] = getMatchingSubfields(
      sierraBibData,
      marcTag = "776",
      marcSubfieldTag = "w"
    ).flatten

    val maybeBibNumbers: List[Option[String]] = matchingSubfields
      .map { _.content }
      .map {
        case uklwPrefixRegex(bibNumber) => Some(bibNumber)
        case _                          => None
      }
      .distinct

    maybeBibNumbers match {
      case List(Some(bibNumber)) =>
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
      case _ => List()
    }
  }

  /** We can merge a single-page Miro and Sierra work if:
    *
    *   - The Sierra work has type "Picture" or "Digital images"
    *   - There's exactly one Miro ID in MARC tag 962 subfield $u
    *     (if there's more than one Miro ID, we can't do a merge).
    *   - There's exactly one item on the Sierra record (if there's more
    *     than one item, we don't know where to put the Miro location).
    *
    */
  private def getSinglePageMiroMergeCandidates(
    sierraBibData: SierraBibData): List[MergeCandidate] = {
    sierraBibData.materialType match {
      // The Sierra material type codes we care about are:
      //
      //    k   Pictures
      //    q   Digital Images
      //
      case Some(SierraMaterialType("k")) | Some(SierraMaterialType("q")) => {
        val matchingSubfields: List[MarcSubfield] = getMatchingSubfields(
          sierraBibData,
          marcTag = "962",
          marcSubfieldTag = "u"
        ).flatten

        val maybeMiroIDs: List[String] = matchingSubfields
          .map { _.content }
          .flatMap { maybeGetMiroID }
          .distinct

        maybeMiroIDs match {
          case List(miroID) =>
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
          case _ => List()
        }
      }
      case _ => List()
    }
  }
}
