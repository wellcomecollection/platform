package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier}
import uk.ac.wellcome.platform.transformer.source.SierraBibData

trait SierraIdentifiers extends SierraCheckDigits {

  // Populate wwork:identifiers.
  //
  //    We populate with two identifier schemes:
  //
  //    -   "sierra-system-number" for the full record type (incl. prefix and
  //        check digit)
  //    -   "sierra-identifier" for the 7-digit interal ID
  //
  //    Adding other identifiers is out-of-scope for now.
  //
  def getIdentifiers(bibData: SierraBibData): List[SourceIdentifier] =
    List(
      SourceIdentifier(
        identifierType = IdentifierType("sierra-system-number"),
        ontologyType = "Work",
        value = addCheckDigit(
          bibData.id,
          recordType = SierraRecordTypes.bibs
        )
      ),
      SourceIdentifier(
        identifierType = IdentifierType("sierra-identifier"),
        ontologyType = "Work",
        value = bibData.id
      )
    )
}
