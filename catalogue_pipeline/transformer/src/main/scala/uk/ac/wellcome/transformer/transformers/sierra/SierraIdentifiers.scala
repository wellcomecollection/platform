package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier}

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
        value = addCheckDigit(
          bibData.id,
          resourceType = SierraRecordTypes.bibs
        )
      ),
      SourceIdentifier(
        identifierScheme = IdentifierSchemes.sierraIdentifier,
        value = bibData.id
      )
    )
}
