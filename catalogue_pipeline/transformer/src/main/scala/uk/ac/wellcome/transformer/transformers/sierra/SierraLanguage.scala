package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.models.Language
import uk.ac.wellcome.transformer.source.{MarcSubfield, SierraBibData}

trait SierraLanguage {

  // Populate wwork:language.
  //
  // We use the "lang" field, if present.
  //
  // Notes:
  //
  //  - "lang" is an optional field in the Sierra API.
  //  - These are populated by ISO 639-2 language codes, but we treat them
  //    as opaque identifiers for our purposes.
  //
  def getLanguage(bibData: SierraBibData): Option[Language] =
    bibData.lang.map { lang =>
      Language(
        id = lang.code,
        label = lang.name
      )
    }
}
