package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.archivist.bag.ArchiveJobCreator
import uk.ac.wellcome.platform.archive.archivist.models.errors.{
  ArchiveError,
  ArchiveJobError
}
import uk.ac.wellcome.platform.archive.archivist.models.{
  ArchiveJob,
  BagUploaderConfig
}
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  IngestBagRequest
}
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressEvent,
  ProgressUpdate
}

object ArchiveZipFileFlow extends Logging {
  def apply(config: BagUploaderConfig, snsConfig: SNSConfig)(
    implicit s3Client: AmazonS3,
    snsClient: AmazonSNS
  ): Flow[ZipFileDownloadComplete,
          Either[ArchiveError[_], ArchiveComplete],
          NotUsed] =
    Flow[ZipFileDownloadComplete].flatMapMerge(
      config.parallelism, {
        case ZipFileDownloadComplete(zipFile, ingestRequest) =>
          Source
            .single(zipFile)
            .log("creating archive job")
            .map(ArchiveJobCreator.create(_, config, ingestRequest))
            .via(
              FoldEitherFlow[
                ArchiveError[IngestBagRequest],
                ArchiveJob,
                Either[ArchiveError[_], ArchiveComplete]
              ](ifLeft = Left(_))(
                ifRight = ArchiveJobFlow(
                  config.bagItConfig.digestDelimiterRegexp,
                  config.parallelism,
                  ingestRequest)))
            .flatMapMerge(
              config.parallelism,
              (result: Either[ArchiveError[_], ArchiveComplete]) =>
                Source
                  .single(toProgressUpdate(result, ingestRequest))
                  .log("sending to progress monitor")
                  .via(SnsPublishFlow[ProgressUpdate](snsClient, snsConfig, Some("archivist_progress")))
                  .map(_ => result)
            )
      }
    )

  private def toProgressUpdate(
    result: Either[ArchiveError[_], ArchiveComplete],
    ingestBagRequest: IngestBagRequest): ProgressUpdate =
    result match {
      case Right(ArchiveComplete(id, _, _)) =>
        ProgressUpdate(
          id,
          List(ProgressEvent("Bag uploaded and verified successfully")))
      case Left(ArchiveJobError(_, errors)) =>
        ProgressUpdate(
          ingestBagRequest.archiveRequestId,
          errors.map(error => ProgressEvent(error.toString)),
          Progress.Failed)
      case Left(archiveError) =>
        ProgressUpdate(
          ingestBagRequest.archiveRequestId,
          List(ProgressEvent(archiveError.toString)),
          Progress.Failed)
    }
}
