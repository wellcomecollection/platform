package uk.ac.wellcome.finatra.modules

/** This is the canonical version of our identifier schemes.  This contains
  *  the strings that will be presented to users of the API.
  */
object IdentifierSchemes {

  // Corresponds to the image number in Miro, e.g. V00127563.
  val miroImageNumber = "miro-image-number"

  // Corresponds to legacy image IDs from the Miro data.  Eventually these
  // should be tackled properly: either assigned proper identifier schemes
  // and normalised, or removed entirely.  For now, this is a stopgap to
  // get these IDs into the API until we deal with them properly.
  val miroLibraryReference = "miro-library-reference"

  // Placeholder until we ingest real Calm records.
  // TODO: Replace this with something more appropriate
  val calmPlaceholder = "calm-placeholder"

  val calmAltRefNo = "calm-altrefno"

  val sierraSystemNumber = "sierra-system-number"
}
