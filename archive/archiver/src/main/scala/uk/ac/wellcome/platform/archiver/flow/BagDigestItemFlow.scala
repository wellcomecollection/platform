package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Framing, Source}
import akka.util.ByteString
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.storage.ObjectLocation

object BagDigestItemFlow extends Logging {

  def apply(config: BagUploaderConfig):
  Flow[(ObjectLocation, String, ZipFile), BagDigestItem, NotUsed] = {

    val framingDelimiter = Framing.delimiter(
      ByteString("\n"),
      maximumFrameLength = 1024,
      allowTruncation = true
    )

    val fileSplitterFlow: Flow[(ObjectLocation, ZipFile), Array[String], NotUsed] = FileExtractorFlow()
      .map(byteString => byteString.concat(ByteString("\n")))
      .via(framingDelimiter)
      .map(_.utf8String)
      .map(_.split(config.digestDelimiter).map(_.trim))

    val bagDigestItemFlow: Flow[(Array[String], String), BagDigestItem, NotUsed] = Flow[(Array[String], String)]
      .map {
        case (Array(checksum: String, key: String), bagName) => {
          val bagDigestItem = BagDigestItem(checksum, ObjectLocation(bagName, key))

          debug(s"Found BagDigestItem: $bagDigestItem in $bagName")

          bagDigestItem
        }
        case (default, bagName) => throw new RuntimeException(
          s"Malformed bag digest line: ${default.mkString(config.digestDelimiter)} in $bagName")
      }

    Flow[(ObjectLocation, String, ZipFile)].flatMapConcat {
      case (objectLocation, bagName, zipFile) =>

        Source.single((objectLocation, zipFile))
          .via(fileSplitterFlow)
          .map(stringArray => (stringArray, bagName))
          .via(bagDigestItemFlow)
    }
  }
}

case class BagDigestItem(checksum: String, location: ObjectLocation)
