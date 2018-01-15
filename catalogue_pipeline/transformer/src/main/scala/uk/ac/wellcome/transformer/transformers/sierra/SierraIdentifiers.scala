package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.{IdentifierSchemes, SourceIdentifier}

import uk.ac.wellcome.transformer.source.SierraBibData

trait SierraIdentifiers {

  // Populate wwork:identifiers.
  //
  //    We populate with "identifierScheme": "sierra-system-number" for the
  //    main ID on the original bib.  Adding other identifiers is out-of-scope
  //    for now.
  //
  def getIdentifiers(bibData: SierraBibData): List[SourceIdentifier] =
    List(
      SourceIdentifier(
        identifierScheme = IdentifierSchemes.sierraSystemNumber,
        value = bibData.id
      )
    )
}
