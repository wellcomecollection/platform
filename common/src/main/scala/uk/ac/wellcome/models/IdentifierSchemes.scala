package uk.ac.wellcome.models

import com.fasterxml.jackson.core.`type`.TypeReference

class IdentifierSchemeType extends TypeReference[IdentifierSchemes.type]

/** This is the canonical version of our identifier schemes.  This contains
  *  the strings that will be presented to users of the API.
  */
object IdentifierSchemes extends Enumeration {
  // Corresponds to the image number in Miro, e.g. V00127563.
  val miroImageNumber = Value("miro-image-number")

  // Corresponds to legacy image IDs from the Miro data.  Eventually these
  // should be tackled properly: either assigned proper identifier schemes
  // and normalised, or removed entirely.  For now, this is a stopgap to
  // get these IDs into the API until we deal with them properly.
  val miroLibraryReference = Value("miro-library-reference")

  // Placeholder until we ingest real Calm records.
  // TODO: Replace this with something more appropriate
  val calmPlaceholder = Value("calm-placeholder")

  val calmAltRefNo = Value("calm-altrefno")

  val sierraSystemNumber = Value("sierra-system-number")
}
