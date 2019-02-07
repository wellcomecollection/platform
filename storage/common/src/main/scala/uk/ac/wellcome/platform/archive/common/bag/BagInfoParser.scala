package uk.ac.wellcome.platform.archive.common.bag

import java.io.InputStream
import java.time.LocalDate

import cats.data._
import cats.implicits._
import uk.ac.wellcome.platform.archive.common.models.bagit._
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
  val externalDescription = "External-Description"
  val internalSenderIdentifier = "Internal-Sender-Identifier"
  val internalSenderDescription = "Internal-Sender-Description"
}

object BagInfoParser {
  private val bagInfoFieldRegex = """(.*?)\s*:\s*(.*)\s*""".r
  private val payloadOxumRegex =
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
      extractPayloadOxum(bagInfoLines),
      extractBaggingDate(bagInfoLines),
      extractSourceOrganisation(bagInfoLines),
      extractExternalDescription(bagInfoLines),
      extractInternalSenderIdentifier(bagInfoLines),
      extractInternalSenderDescription(bagInfoLines)
    ).mapN(
      (externalIdentifier: ExternalIdentifier,
       payloadOxum: PayloadOxum,
       baggingDate: LocalDate,
       sourceOrganisation: Option[SourceOrganisation],
       externalDescription: Option[ExternalDescription],
       internalSenderIdentifier: Option[InternalSenderIdentifier],
       internalSenderDescription: Option[InternalSenderDescription]) =>
        BagInfo.apply(
          externalIdentifier,
          payloadOxum,
          baggingDate,
          sourceOrganisation,
          externalDescription,
          internalSenderIdentifier,
          internalSenderDescription))

    inputStream.close()

    validated.toEither.leftMap(list => InvalidBagInfo(t, list.toList))
  }

  private def extractExternalIdentifier(bagInfoLines: Array[String]) = {
    extractRequiredValue(bagInfoLines, BagInfoKeys.externalIdentifier)
      .map(ExternalIdentifier.apply)
  }

  private def extractPayloadOxum(bagInfoLines: Array[String]) = {
    bagInfoLines
      .collectFirst {
        case payloadOxumRegex(bytes, numberOfFiles) =>
          PayloadOxum(bytes.toLong, numberOfFiles.toInt)
      }
      .toValidNel(BagInfoKeys.payloadOxum)

  }

  private def extractBaggingDate(bagInfoLines: Array[String]) = {
    extractRequiredValue(bagInfoLines, BagInfoKeys.baggingDate).andThen(
      dateString =>
        Try(LocalDate.parse(dateString)).toEither
          .leftMap(_ => BagInfoKeys.baggingDate)
          .toValidatedNel)
  }

  private def extractSourceOrganisation(bagInfoLines: Array[String]) = {
    extractOptionalValue(bagInfoLines, BagInfoKeys.sourceOrganisation)
      .map(SourceOrganisation)
      .validNel
  }

  private def extractExternalDescription(bagInfoLines: Array[String]) =
    extractOptionalValue(bagInfoLines, BagInfoKeys.externalDescription)
      .map(ExternalDescription)
      .validNel

  private def extractInternalSenderIdentifier(bagInfoLines: Array[String]) =
    extractOptionalValue(bagInfoLines, BagInfoKeys.internalSenderIdentifier)
      .map(InternalSenderIdentifier)
      .validNel

  private def extractInternalSenderDescription(bagInfoLines: Array[String]) =
    extractOptionalValue(bagInfoLines, BagInfoKeys.internalSenderDescription)
      .map(InternalSenderDescription)
      .validNel

  private def extractRequiredValue(bagInfoLines: Array[String],
                                   bagInfoKey: String) = {
    extractOptionalValue(bagInfoLines, bagInfoKey)
      .toValidNel(bagInfoKey)
  }

  private def extractOptionalValue(bagInfoLines: Array[String],
                                   bagInfoKey: String) = {
    bagInfoLines
      .collectFirst {
        case bagInfoFieldRegex(key, value) if key == bagInfoKey =>
          value
      }
  }
}
