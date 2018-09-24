package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveJob, BagUploaderConfig}
import uk.ac.wellcome.platform.archive.common.models.ArchiveComplete
import uk.ac.wellcome.platform.archive.common.progress.monitor.ArchiveProgressMonitor

object ArchiveZipFileFlow extends Logging {
  def apply(config: BagUploaderConfig)(
    implicit s3Client: AmazonS3,
    archiveProgressMonitor: ArchiveProgressMonitor,
    actorSystem: ActorSystem
  ): Flow[ZipFileDownloadComplete, ArchiveComplete, NotUsed] = {

    Flow[ZipFileDownloadComplete].flatMapConcat {
      case ZipFileDownloadComplete(zipFile, ingestRequest) => {
        Source
          .single(zipFile)
          .mapConcat(ArchiveJob.create(_, config))
          // TODO: Log error here
          .via(ArchiveBagFlow(config.bagItConfig.digestDelimiterRegexp))
          .map(job =>
            ArchiveComplete(
              job.bagLocation,
              ingestRequest
            )
          )

      }
    }
  }
}