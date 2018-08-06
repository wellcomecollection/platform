package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Framing, Source}
import akka.util.ByteString
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.storage.ObjectLocation

object BagDigestItemFlow extends Logging {
  def apply(config: BagUploaderConfig)
    : Flow[(ObjectLocation, BagName, ZipFile), BagDigestItem, NotUsed] = {

    val framingDelimiter = Framing.delimiter(
      ByteString("\n"),
      maximumFrameLength = 1024,
      allowTruncation = true
    )

    val fileSplitterFlow
      : Flow[(ObjectLocation, ZipFile), Array[String], NotUsed] =
      FileExtractorFlow()
        .map(byteString => byteString.concat(ByteString("\n")))
        .via(framingDelimiter)
        .map(_.utf8String)
        .filter(_.nonEmpty)
        .map(_.split(config.digestDelimiter).map(_.trim))

    val bagDigestItemFlow
      : Flow[(Array[String], BagName), BagDigestItem, NotUsed] =
      Flow[(Array[String], BagName)]
        .map {
          case (Array(checksum: String, key: String), bagName) =>
            BagDigestItem(checksum, ObjectLocation(bagName.value, key))
          case (default, bagName) =>
            throw MalformedBagDigestException(
              default.mkString(config.digestDelimiter),
              bagName
            )
        }

    Flow[(ObjectLocation, BagName, ZipFile)]
      .log("digest location")
      .flatMapConcat {
        case (objectLocation, bagName, zipFile) =>
          Source
            .single((objectLocation, zipFile))
            .via(fileSplitterFlow)
            .map(stringArray => (stringArray, bagName))
            .via(bagDigestItemFlow)
      }
      .log("bag digest item")
  }
}

case class BagDigestItem(checksum: String, location: ObjectLocation)

case class MalformedBagDigestException(line: String, bagName: BagName)
    extends RuntimeException(
      s"Malformed bag digest line: $line in ${bagName.value}")
