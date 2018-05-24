package uk.ac.wellcome.models.work.internal

import java.io.InputStream

import io.circe.{Decoder, Encoder, HCursor, Json}

import scala.io.Source

//object IdentifierSchemesResour

object NewIdentifierSchemes {
  private val stream: InputStream = getClass
    .getResourceAsStream("/identifier-schemes.csv")

  // identifier-schemes.csv is a CSV file with three columns per row:
  //
  //    CALMRefNo,calm-ref-no,CALM ref no
  //
  // The first entry is an immutable platform identifier.  The second
  // and third entries are the ID and label we show in public ID schemes.
  //
  // Internally, we use the platform identifier ("CALMRefNo") -- this won't
  // change even if the ID ("calm-ref-no") or label ("CALM ref no") do.
  //
  private val csvRows: Array[(String, String, String)] = Source.fromInputStream(stream).mkString
    .split("\n")
    .map { row =>
      val columns = row.split(",").map(_.trim)
      assert(columns.length == 3)
      (columns(0), columns(1), columns(2))
    }

  private val identifierSchemeMap: Map[String, (String, String)] = csvRows
      .map { case (platformId, schemeId, schemeLabel) =>
        Map(platformId -> Tuple2(schemeId, schemeLabel))
      }
      .fold(Map[String, (String, String)]()) { (x, y) => x ++ y}

  def getIdentifierScheme(platformId: String): SourceIdentifier = {
    println(identifierSchemeMap)

    SourceIdentifier(
      identifierScheme = IdentifierSchemes.miroImageNumber,
      ontologyType = "Work",
      value = "foo"
    )
  }
}

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
    medicalSubjectHeadings
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
