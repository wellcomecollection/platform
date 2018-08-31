package uk.ac.wellcome.platform.archive.archivist.flow

import java.util.zip.ZipFile

import akka.stream.scaladsl.{Flow, Framing, Source}
import akka.util.ByteString
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.models.{BagContentItem, BagName}
import uk.ac.wellcome.storage.ObjectLocation

object BagDigestItemFlow extends Logging {

  val framingDelimiter = Framing.delimiter(
    ByteString("\n"),
    maximumFrameLength = 1024,
    allowTruncation = true
  )

  def apply(digestDelimiter: String) =
    Flow[(ObjectLocation, BagName, ZipFile)]
      .log("digest location")
      .flatMapConcat {
        case (objectLocation, bagName, zipFile) =>
          Source
            .single((objectLocation, zipFile))
            .via(FileExtractorFlow())
            .via(framingDelimiter)
            .map(_.utf8String)
            .filter(_.nonEmpty)
            .map(createBagDigestItems(_, bagName, digestDelimiter))
      }
      .log("bag digest item")

  private def createBagDigestItems(
    fileChunk: String,
    bagName: BagName,
    delimiter: String
  ) = {
    val splitChunk = fileChunk.split(delimiter).map(_.trim)

    splitChunk match {
      case Array(checksum: String, key: String) =>
        BagContentItem(
          checksum,
          ObjectLocation(bagName.value, key)
        )
      case default =>
        throw MalformedBagDigestException(
          default.mkString(delimiter),
          bagName
        )
    }
  }
}

case class MalformedBagDigestException(line: String, bagName: BagName)
    extends RuntimeException(
      s"Malformed bag digest line: $line in ${bagName.value}")
