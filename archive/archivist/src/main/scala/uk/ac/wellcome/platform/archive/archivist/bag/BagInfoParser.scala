package uk.ac.wellcome.platform.archive.archivist.bag
import java.io.InputStream

import uk.ac.wellcome.platform.archive.archivist.models.errors.InvalidBagInfo
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.models.{BagInfo, ExternalIdentifier, PayloadOxum, SourceOrganisation}
import cats.data._
import cats.data.Validated._
import cats.implicits._


object BagInfoParser {
  val bagInfoFieldRegex = """(.*?)\s*:\s*(.*)\s*""".r
  val payloadOxumRegex = """Payload-Oxum\s*:\s*([0-9]+)\.([0-9]+)\s*""".r

  def parseBagInfo[T](t: T, inputStream: InputStream): Either[ArchiveError[T],BagInfo] = {
    val bagInfoLines = scala.io.Source
      .fromInputStream(inputStream, "UTF-8")
      .mkString
      .split("\n")

    val validated: ValidatedNel[String, BagInfo] =(
      extractExternalIdentifier(bagInfoLines),
    extractSourceOrganisation(bagInfoLines),
    extractPayloadOxum(bagInfoLines))
      .mapN(BagInfo.apply)

    validated.toEither.leftMap(list => InvalidBagInfo(t, list.toList))
  }

  private def extractPayloadOxum(
    bagInfoLines: Array[String]) ={
    bagInfoLines
      .collectFirst {
        case payloadOxumRegex(bytes, numberOfFiles) =>
          PayloadOxum(bytes.toLong, numberOfFiles.toInt)
      }.toValidNel("Payload-Oxum")

    }

  private def extractSourceOrganisation(
    bagInfoLines: Array[String]) = {
    extractValue(bagInfoLines, "Source-Organization").map(
      SourceOrganisation.apply)
  }

  private def extractExternalIdentifier(
    bagInfoLines: Array[String]) = {
    extractValue(bagInfoLines, "External-Identifier").map(
      ExternalIdentifier.apply)
  }

private def extractValue(bagInfoLines: Array[String], bagInfoKey: String) = {
    bagInfoLines
      .collectFirst {
        case bagInfoFieldRegex(key, value) if key == bagInfoKey =>
          value
      }.toValidNel(bagInfoKey)
  }
}
