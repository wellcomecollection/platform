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
import uk.ac.wellcome.platform.archive.archivist.models.{
  BagUploaderConfig,
  FileDownloadComplete
}
import uk.ac.wellcome.platform.archive.archivist.models.TypeAliases.{
  ArchiveCompletion,
  BagDownload
}
import uk.ac.wellcome.platform.archive.archivist.models.errors.ArchiveJobError
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.models.{
  IngestBagRequest,
  ReplicationRequest
}
import uk.ac.wellcome.platform.archive.common.progress.models._

object ArchiveZipFileFlow extends Logging {

  def apply(config: BagUploaderConfig, snsConfig: SNSConfig)(
    implicit s3Client: AmazonS3,
    snsClient: AmazonSNS
  ): Flow[BagDownload, ArchiveCompletion, NotUsed] = {
    Flow[BagDownload].flatMapMerge(
      config.parallelism, {
        case Left(error) => Source.single(Left(error))
        case Right(FileDownloadComplete(file, ingestRequest)) =>
          Source
            .single(new ZipFile(file))
            .map(ArchiveJobCreator.create(_, config, ingestRequest))
            .flatMapMerge(
              config.parallelism, {
                case Left(error) => Source.single(Left(error))
                case Right(archiveJob) =>
                  Source
                    .single(archiveJob)
                    .via(
                      ArchiveJobFlow(
                        parallelism = config.parallelism,
                        ingestBagRequest = ingestRequest))
                    .map(deleteFile(_, file))
              }
            )
            .flatMapMerge(
              config.parallelism,
              (result: ArchiveCompletion) =>
                Source
                  .single(toProgressUpdate(result, ingestRequest))
                  .via(
                    SnsPublishFlow[ProgressUpdate](
                      snsClient,
                      snsConfig,
                      subject = "archivist_progress"))
                  .map(_ => result)
            )
      }
    )
  }

  private def deleteFile(
    passContext: ArchiveCompletion,
    file: File
  ) = {
    debug(s"Deleting file ${file.getName}")

    file.delete()

    passContext
  }

  private def toProgressUpdate(
    result: Either[ArchiveError[_], ReplicationRequest],
    ingestBagRequest: IngestBagRequest): ProgressUpdate = {
    result match {
      case Right(ReplicationRequest(id, _)) =>
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
}
