package uk.ac.wellcome.platform.archive.archivist.flow

import java.io.File
import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.archivist.bag.ArchiveJobCreator
import uk.ac.wellcome.platform.archive.archivist.models.BagUploaderConfig
import uk.ac.wellcome.platform.archive.archivist.models.errors.ArchiveJobError
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  FileDownloadComplete,
  IngestBagRequest,
  Parallelism
}
import uk.ac.wellcome.platform.archive.common.progress.models._

object ArchiveZipFileFlow extends Logging {

  type ArchiveBagJob = Either[ArchiveError[_], ArchiveComplete]
  type BagDownload =
    Either[ArchiveError[IngestBagRequest], FileDownloadComplete]

  def apply(config: BagUploaderConfig, snsConfig: SNSConfig)(
    implicit s3Client: AmazonS3,
    snsClient: AmazonSNS,
    parallelism: Parallelism
  ): Flow[BagDownload, ArchiveBagJob, NotUsed] =
    Flow[BagDownload].flatMapMerge(
      parallelism.value, {
        case Left(error) => Source.single(Left(error))
        case Right(FileDownloadComplete(file, ingestRequest)) =>
          val archiveJobFlow = ArchiveJobFlow(
            delimiter = config.bagItConfig.digestDelimiterRegexp,
            ingestBagRequest = ingestRequest
          )

          Source
            .single(file)
            .map(ArchiveJobCreator.create(_, config, ingestRequest))
            .via(archiveJobFlow)
            .map(deleteZipFile(_, new ZipFile(file)))
            .flatMapMerge(
              config.parallelism,
              (result: Either[ArchiveError[_], ArchiveComplete]) =>
                Source
                  .single(toProgressUpdate(result, ingestRequest))
                  .log("sending to progress monitor")
                  .via(
                    SnsPublishFlow[ProgressUpdate](
                      snsClient,
                      snsConfig,
                      subject = "archivist_progress"))
                  .map(_ => result)
            )

      }
    )

  private def deleteZipFile(
    passContext: Either[ArchiveError[_], ArchiveComplete],
    zipFile: ZipFile) = {
    debug(s"Deleting zipfile ${zipFile.getName}")
    new File(zipFile.getName).delete()
    passContext
  }

  private def toProgressUpdate(
    result: Either[ArchiveError[_], ArchiveComplete],
    ingestBagRequest: IngestBagRequest): ProgressUpdate =
    result match {
      case Right(ArchiveComplete(id, _, _)) =>
        ProgressEventUpdate(
          id,
          List(ProgressEvent("Bag uploaded and verified successfully")))
      case Left(ArchiveJobError(_, errors)) =>
        ProgressStatusUpdate(
          ingestBagRequest.id,
          Progress.Failed,
          None,
          errors.map(error => ProgressEvent(error.toString)))
      case Left(archiveError) =>
        ProgressStatusUpdate(
          ingestBagRequest.id,
          Progress.Failed,
          None,
          List(ProgressEvent(archiveError.toString)))
    }
}
