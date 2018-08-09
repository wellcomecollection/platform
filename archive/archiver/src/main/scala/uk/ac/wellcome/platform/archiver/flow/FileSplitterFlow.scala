package uk.ac.wellcome.platform.archiver.flow

import akka.stream.scaladsl.Framing
import akka.util.ByteString
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig

object FileSplitterFlow {
  def apply(config: BagUploaderConfig) = {
    val framingDelimiter = Framing.delimiter(
      ByteString("\n"),
      maximumFrameLength = 1024,
      allowTruncation = true
    )

    FileExtractorFlow()
      .via(framingDelimiter)
      .map(_.utf8String)
      .filter(_.nonEmpty)
      .map(_.split(config.digestDelimiter).map(_.trim))
  }
}
