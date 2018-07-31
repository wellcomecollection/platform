package uk.ac.wellcome.platform.archiver.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Framing}
import akka.util.ByteString
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.storage.ObjectLocation

object BagDigestItemFlow {

  def apply(config: BagUploaderConfig, bagName: String, zipFile: ZipFile):
  Flow[ObjectLocation, BagDigestItem, NotUsed] = {

    val framingDelimiter = Framing.delimiter(
      ByteString("\n"),
      maximumFrameLength = 1024,
      allowTruncation = true
    )

    FileExtractorFlow(zipFile)
      .map(byteString => byteString.concat(ByteString("\n")))
      .via(framingDelimiter)
      .map(_.utf8String)
      .map(_.split(config.digestDelimiter).map(_.trim))
      .map {
        case Array(checksum: String, key: String) => BagDigestItem(checksum, ObjectLocation(bagName, key))
        case default => throw new RuntimeException(s"Malformed bag digest line: ${default.mkString(config.digestDelimiter)}")
      }
  }
}

case class BagDigestItem(checksum: String, location: ObjectLocation)
