package uk.ac.wellcome.platform.archive.archivist.streams.flow

import java.util.zip.ZipFile

import akka.stream.scaladsl.{Flow, Framing, Source}
import akka.util.ByteString
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveItemJob, ArchiveJob, DigestLocation}
import uk.ac.wellcome.platform.archive.common.models.{BagItem, BagPath}

object BagDigestItemFlow extends Logging {

  val framingDelimiter = Framing.delimiter(
    ByteString("\n"),
    maximumFrameLength = 1024,
    allowTruncation = true
  )

  def apply(digestDelimiter: String) =
    Flow[(DigestLocation, ArchiveJob)]
      .log("digest location")
      .flatMapConcat {
        case (digestLocation, job@ArchiveJob(zipFile, bagLocation, _)) =>
          Source
            .single((digestLocation.toObjectLocation, zipFile))
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


