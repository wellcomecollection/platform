package uk.ac.wellcome.platform.archive.archivist.flow

import akka.stream.scaladsl.{Flow, Framing, Source}
import akka.util.ByteString
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ArchiveJob, ZipLocation}
import uk.ac.wellcome.platform.archive.common.models.BagItem

object BagDigestItemFlow extends Logging {

  val framingDelimiter = Framing.delimiter(
    ByteString("\n"),
    maximumFrameLength = 1024,
    allowTruncation = true
  )

  def apply(digestDelimiter: String) =
    Flow[ArchiveJob]
      .log("digest location")
      .flatMapConcat {
        case job@ArchiveJob(zipFile, bagLocation, _, manifestLocation) =>
          Source
            .single(ZipLocation(zipFile, manifestLocation.toObjectLocation))
            .via(ZipFileEntryFlow())
            .via(framingDelimiter)
            .map(_.utf8String)
            .filter(_.nonEmpty)
            .map(checksum =>
              ArchiveItemJob(job, BagItem(checksum, bagLocation.bagPath, digestDelimiter))
            )
      }
      .log("bag digest item")

}


