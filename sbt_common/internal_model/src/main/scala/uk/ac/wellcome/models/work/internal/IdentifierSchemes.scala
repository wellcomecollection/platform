package uk.ac.wellcome.models.work.internal

import io.circe.{Decoder, Encoder, HCursor, Json}

/** This is the canonical version of our identifier schemes.  This contains
  *  the strings that will be presented to users of the API.
  */
object IdentifierSchemes {
  sealed trait IdentifierScheme

  // Corresponds to the image number in Miro, e.g. V00127563.
  case object miroImageNumber extends IdentifierScheme {
    override def toString: String = "miro-image-number"
  }

  // Corresponds to legacy image IDs from the Miro data.  Eventually these
  // should be tackled properly: either assigned proper identifier schemes
  // and normalised, or removed entirely.  For now, this is a stopgap to
  // get these IDs into the API until we deal with them properly.
  case object miroLibraryReference extends IdentifierScheme {
    override def toString: String = "miro-library-reference"
  }

  // Placeholder until we ingest real Calm records.
  // TODO: Replace this with something more appropriate
  case object calmPlaceholder extends IdentifierScheme {
    override def toString: String = "calm-placeholder"
  }

  case object calmAltRefNo extends IdentifierScheme {
    override def toString: String = "calm-altrefno"
  }

  // Represents the full form of a Sierra record number, including the record
  // type prefix ("b" for bibs, "i" for items, etc.) and the check digit.
  // Examples: b17828636, i17832846
  case object sierraSystemNumber extends IdentifierScheme {
    override def toString: String = "sierra-system-number"
  }

  // Represents a combined work resulting from merging related works
  // Examples: ??
  case object mergedWork extends IdentifierScheme {
    override def toString: String = "merged-work"
  }

  // Represents the internal form of a Sierra record number, with no record
  // type prefix or check digit.  7 digits.
  // Examples: 1782863, 1783284
  case object sierraIdentifier extends IdentifierScheme {
    override def toString: String = "sierra-identifier"
  }

  case object marcCountries extends IdentifierScheme {
    override def toString: String = "marc-countries"
  }

  // Note: these are two different schemes.  The Library of Congress (LC)
  // publishes Subject Headings and a Name Authority File, and they aren't
  // the same!
  case object libraryOfCongressNames extends IdentifierScheme {
    override def toString: String = "library-of-congress-names"
  }

  case object libraryOfCongressSubjectHeadings extends IdentifierScheme {
    override def toString: String = "library-of-congress-subject-headings"
  }

  case object medicalSubjectHeadings extends IdentifierScheme {
    override def toString: String = "medical-subject-headings"
  }

  private final val knownIdentifierSchemes = Seq(
    miroImageNumber,
    miroLibraryReference,
    calmPlaceholder,
    calmAltRefNo,
    sierraSystemNumber,
    sierraIdentifier,
    libraryOfCongressNames,
    libraryOfCongressSubjectHeadings,
    marcCountries,
    medicalSubjectHeadings,
    mergedWork
  )

  private def createIdentifierScheme(
    identifierScheme: String): IdentifierSchemes.IdentifierScheme = {
    knownIdentifierSchemes
      .find(_.toString == identifierScheme)
      .getOrElse {
        val errorMessage = s"$identifierScheme is not a valid identifierScheme"
        throw new Exception(errorMessage)
      }
  }

  implicit val identifierSchemesDecoder
    : Decoder[IdentifierSchemes.IdentifierScheme] =
    new Decoder[IdentifierSchemes.IdentifierScheme] {
      final def apply(
        c: HCursor): Decoder.Result[IdentifierSchemes.IdentifierScheme] = {
        for {
          identifierSchemeName <- c.as[String]
        } yield {
          IdentifierSchemes.createIdentifierScheme(identifierSchemeName)
        }
      }
    }

  implicit val identifierSchemesEncoder
    : Encoder[IdentifierSchemes.IdentifierScheme] =
    new Encoder[IdentifierSchemes.IdentifierScheme] {
      override def apply(a: IdentifierScheme): Json = {
        Json.fromString(a.toString)
      }
    }

}
