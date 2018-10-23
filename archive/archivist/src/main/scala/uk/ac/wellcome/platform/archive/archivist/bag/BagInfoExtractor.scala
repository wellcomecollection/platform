package uk.ac.wellcome.platform.archive.archivist.bag
import java.io.InputStream

import uk.ac.wellcome.platform.archive.archivist.models.errors.InvalidBagInfo
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.models.{BagInfo, ExternalIdentifier, SourceOrganisation}
import cats.data._
import cats.data.Validated._
import cats.implicits._


object BagInfoExtractor {
  val regex = """(.*?)\s*:\s*(.*)\s*""".r

  def extractBagInfo[T](t: T, inputStream: InputStream): Either[ArchiveError[T],BagInfo] = {
    val bagInfoLines = scala.io.Source
      .fromInputStream(inputStream, "UTF-8")
      .mkString
      .split("\n")

    val validated: ValidatedNel[String, BagInfo] =(
      extractExternalIdentifier(bagInfoLines),
    extractSourceOrganisation(bagInfoLines))
      .mapN(BagInfo.apply)

    validated.toEither.leftMap(list => InvalidBagInfo(t, list.toList))

  }
  private def extractSourceOrganisation[T](
    bagInfoLines: Array[String]) = {
    extractValue(bagInfoLines, "Source-Organization").map(
      SourceOrganisation.apply)
  }
  private def extractExternalIdentifier[T](
    bagInfoLines: Array[String]) = {
    extractValue(bagInfoLines, "External-Identifier").map(
      ExternalIdentifier.apply)
  }
private def extractValue[T](bagInfoLines: Array[String], bagInfoKey: String) = {
    bagInfoLines
      .collectFirst {
        case regex(key, value) if key == bagInfoKey =>
          value
      }.toValidNel(bagInfoKey)
  }
}
