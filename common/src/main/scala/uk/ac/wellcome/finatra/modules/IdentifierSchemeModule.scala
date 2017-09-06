package uk.ac.wellcome.finatra.modules

/** This is the canonical version of our identifier schemes.  This contains
  *  the strings that will be presented to users of the API.
  */
object IdentifierSchemes {

  // Corresponds to the image number in Miro, e.g. V00127563.
  val miroImageNumber = "miro-image-number"

  // Placeholder until we ingest real Calm records.
  // TODO: Replace this with something more appropriate
  val calmPlaceholder = "calm-placeholder"

  val calmAltRefNo = "calm-altrefno"

  val sierraSystemNumber = "sierra-system-number"
}
