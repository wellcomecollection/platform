package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.archivist.bag.ArchiveJobCreator
import uk.ac.wellcome.platform.archive.archivist.models.BagUploaderConfig
import uk.ac.wellcome.platform.archive.common.models.ArchiveComplete

object ArchiveZipFileFlow extends Logging {
  def apply(config: BagUploaderConfig)(
    implicit s3Client: AmazonS3
  ): Flow[ZipFileDownloadComplete, ArchiveComplete, NotUsed] =
    Flow[ZipFileDownloadComplete].flatMapMerge(
      config.parallelism, {
        case ZipFileDownloadComplete(zipFile, ingestRequest) =>
          // TODO report progress here
          Source
            .single(zipFile)
            .log("creating archive job")
            .map(ArchiveJobCreator.create(_, config))
            .collect { case Right(job) => job }
            .via(ArchiveJobFlow(
              config.bagItConfig.digestDelimiterRegexp,
              config.parallelism))
            .collect { case Right(archiveJob) => archiveJob }
            .map(
              job =>
                ArchiveComplete(
                  job.bagLocation,
                  ingestRequest
              ))
      }
    )
}
