package uk.ac.wellcome.models

/** This is the canonical version of our identifier schemes.  This contains
  *  the strings that will be presented to users of the API.
  */
object IdentifierSchemes {

  sealed trait IdentifierScheme
  // Corresponds to the image number in Miro, e.g. V00127563.
  case object miroImageNumber extends IdentifierScheme{
    override def toString: String = "miro-image-number"
  }

  // Corresponds to legacy image IDs from the Miro data.  Eventually these
  // should be tackled properly: either assigned proper identifier schemes
  // and normalised, or removed entirely.  For now, this is a stopgap to
  // get these IDs into the API until we deal with them properly.
  case object miroLibraryReference extends IdentifierScheme{
    override def toString: String = "miro-library-reference"
  }

  // Placeholder until we ingest real Calm records.
  // TODO: Replace this with something more appropriate
  case object calmPlaceholder extends IdentifierScheme{
    override def toString: String = "calm-placeholder"
  }

  case object calmAltRefNo extends IdentifierScheme{
    override def toString: String = "calm-altrefno"
  }

  case object sierraSystemNumber extends IdentifierScheme{
    override def toString: String = "sierra-system-number"
  }
}
