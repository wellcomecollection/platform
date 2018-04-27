package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal.{IdentifierSchemes, SourceIdentifier}
import uk.ac.wellcome.transformer.source.SierraBibData

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
        identifierScheme = IdentifierSchemes.sierraSystemNumber,
        ontologyType = "Work",
        value = addCheckDigit(
          bibData.id,
          recordType = SierraRecordTypes.bibs
        )
      ),
      SourceIdentifier(
        identifierScheme = IdentifierSchemes.sierraIdentifier,
        ontologyType = "Work",
        value = bibData.id
      )
    )
}
