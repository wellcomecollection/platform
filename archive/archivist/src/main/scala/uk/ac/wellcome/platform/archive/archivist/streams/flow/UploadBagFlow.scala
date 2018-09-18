package uk.ac.wellcome.platform.archive.archivist.streams.flow

import java.util.zip.ZipFile

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, Source}
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.models.{ArchiveJob, BagUploaderConfig}
import uk.ac.wellcome.platform.archive.common.models.{BagLocation, IngestRequestContext}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ArchiveProgressMonitor

object UploadBagFlow extends Logging {
  def apply(config: BagUploaderConfig)(
    implicit
    s3Client: S3Client,
    archiveProgressMonitor: ArchiveProgressMonitor,
    actorSystem: ActorSystem
  ): Flow[(ZipFile, IngestRequestContext), (BagLocation, IngestRequestContext), NotUsed] = {

    Flow[(ZipFile, IngestRequestContext)].flatMapConcat {
      case (zipFile, ingestRequestContext) => {
        Source
          .single(zipFile)
          .map(ArchiveJob.create(_, config))
          .collect { case Some(archiveJob) => archiveJob }
          .via(ArchiveBagFlow(config.bagItConfig.digestDelimiterRegexp))
          .map(job =>
            (job.bagLocation, ingestRequestContext)
          )

      }
    }
  }
}