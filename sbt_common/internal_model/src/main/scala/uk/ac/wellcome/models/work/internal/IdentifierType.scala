package uk.ac.wellcome.models.work.internal

import java.io.InputStream

import uk.ac.wellcome.exceptions.GracefulFailureException

import scala.io.Source

case class IdentifierType(
  id: String,
  label: String,
  ontologyType: String = "IdentifierType"
)

case object IdentifierType {
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
  private val csvRows: Array[(String, String, String)] = Source
    .fromInputStream(stream)
    .mkString
    .split("\n")
    .map { row =>
      val columns = row.split(",").map(_.trim)
      assert(columns.length == 3)
      (columns(0), columns(1), columns(2))
    }

  private val identifierTypeMap = csvRows
    .map {
      case (platformId, schemeId, schemeLabel) =>
        Map(
          platformId -> IdentifierType(
            id = schemeId,
            label = schemeLabel
          )
        )
    }
    .fold(Map[String, IdentifierType]()) { (x, y) =>
      x ++ y
    }

  private def lookupPlatformId(id: String): IdentifierType =
    identifierTypeMap.get(id) match {
      case Some(identifierType) => identifierType
      case None =>
        throw GracefulFailureException(
          new RuntimeException(s"Unrecognised identifier type: [$id]")
        )
    }

  // This isn't ideal, because we're duplicating the list from the CSV,
  // but it gives us a bit of type-safety.
  //
  // If any of these values don't resolve, we fail immediately upon
  // startup -- not when we try to load the value!
  //
  final val miroImageNumber = lookupPlatformId("MiroImageNumber")
  final val wellcomeLibraryVideoDiskNumber = lookupPlatformId(
    "WellcomeLibraryVideodiskNumber")
  final val sierraSystemNumber = lookupPlatformId("SierraSystemNumber")
  final val calmRefNo = lookupPlatformId("CALMRefNo")
  final val calmAltRefNo = lookupPlatformId("CALMAltRefNo")
  final val lcGraphicMaterials = lookupPlatformId("LCGMGPC")
  final val lcSubjectHeadings = lookupPlatformId("lcsh")
  final val lcNames = lookupPlatformId("LCNames")
  final val mesh = lookupPlatformId("MESHId")
  final val calmRecordId = lookupPlatformId("CALMRecordId")
  final val preservicaGUID = lookupPlatformId("PreservicaGUID")
  final val calmAuthorityCode = lookupPlatformId("CALMAuthorityCode")
  final val isbn = lookupPlatformId("ISBN")
  final val issn = lookupPlatformId("ISSN")
}
