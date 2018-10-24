package uk.ac.wellcome.platform.archive.common.bag

import java.io.InputStream
import java.time.LocalDate

import cats.data._
import cats.implicits._
import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.platform.archive.common.models.error.{
  ArchiveError,
  InvalidBagInfo
}

import scala.util.Try

object BagInfoKeys {
  val externalIdentifier = "External-Identifier"
  val baggingDate = "Bagging-Date"
  val payloadOxum = "Payload-Oxum"
  val sourceOrganisation = "Source-Organization"
}

object BagInfoParser {
  val bagInfoFieldRegex = """(.*?)\s*:\s*(.*)\s*""".r
  val payloadOxumRegex =
    s"""${BagInfoKeys.payloadOxum}\\s*:\\s*([0-9]+)\\.([0-9]+)\\s*""".r

  def parseBagInfo[T](
    t: T,
    inputStream: InputStream): Either[ArchiveError[T], BagInfo] = {
    val bagInfoLines = scala.io.Source
      .fromInputStream(inputStream, "UTF-8")
      .mkString
      .split("\n")

    val validated: ValidatedNel[String, BagInfo] = (
      extractExternalIdentifier(bagInfoLines),
      extractSourceOrganisation(bagInfoLines),
      extractPayloadOxum(bagInfoLines),
      extractBaggingDate(bagInfoLines))
      .mapN(BagInfo.apply)

    validated.toEither.leftMap(list => InvalidBagInfo(t, list.toList))
  }

  private def extractBaggingDate(bagInfoLines: Array[String]) = {
    extractValue(bagInfoLines, BagInfoKeys.baggingDate).andThen(
      dateString =>
        Try(LocalDate.parse(dateString)).toEither
          .leftMap(_ => BagInfoKeys.baggingDate)
          .toValidatedNel)
  }

  private def extractPayloadOxum(bagInfoLines: Array[String]) = {
    bagInfoLines
      .collectFirst {
        case payloadOxumRegex(bytes, numberOfFiles) =>
          PayloadOxum(bytes.toLong, numberOfFiles.toInt)
      }
      .toValidNel(BagInfoKeys.payloadOxum)

  }

  private def extractSourceOrganisation(bagInfoLines: Array[String]) = {
    extractValue(bagInfoLines, BagInfoKeys.sourceOrganisation)
      .map(SourceOrganisation.apply)
  }

  private def extractExternalIdentifier(bagInfoLines: Array[String]) = {
    extractValue(bagInfoLines, BagInfoKeys.externalIdentifier)
      .map(ExternalIdentifier.apply)
  }

  private def extractValue(bagInfoLines: Array[String], bagInfoKey: String) = {
    bagInfoLines
      .collectFirst {
        case bagInfoFieldRegex(key, value) if key == bagInfoKey =>
          value
      }
      .toValidNel(bagInfoKey)
  }
}
