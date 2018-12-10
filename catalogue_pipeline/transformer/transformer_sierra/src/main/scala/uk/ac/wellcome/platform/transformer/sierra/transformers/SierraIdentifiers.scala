package uk.ac.wellcome.platform.transformer.sierra.transformers

import uk.ac.wellcome.models.transformable.sierra.SierraBibNumber
import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier}
import uk.ac.wellcome.platform.transformer.sierra.source.SierraBibData

trait SierraIdentifiers extends MarcUtils {

  // Populate wwork:identifiers.
  //
  //    We populate with three identifier schemes:
  //
  //    -   "sierra-system-number" for the full record type (incl. prefix and
  //        check digit)
  //    -   "sierra-identifier" for the 7-digit internal ID
  //    -   "isbn" from MARC tag 020 subfield $a.  This is repeatable.
  //        https://www.loc.gov/marc/bibliographic/bd020.html
  //
  //    Adding other identifiers is out-of-scope for now.
  //
  def getOtherIdentifiers(bibId: SierraBibNumber,
                          bibData: SierraBibData): List[SourceIdentifier] = {
    val sierraIdentifier = SourceIdentifier(
      identifierType = IdentifierType("sierra-identifier"),
      ontologyType = "Work",
      value = bibId.withoutCheckDigit
    )

    val isbnIdentifiers: List[SourceIdentifier] =
      getMatchingSubfields(
        bibData = bibData,
        marcTag = "020",
        marcSubfieldTag = "a"
      ).flatten
        .map { _.content }
        .map { value =>
          SourceIdentifier(
            identifierType = IdentifierType("isbn"),
            ontologyType = "Work",
            value = value
          )
        }

    List(sierraIdentifier) ++ isbnIdentifiers
  }
}
