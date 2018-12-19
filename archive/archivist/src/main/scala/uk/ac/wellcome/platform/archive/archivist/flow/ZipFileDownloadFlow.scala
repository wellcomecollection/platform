package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.ActorAttributes
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.{
  FileDownloadComplete,
  IngestBagRequest,
  Parallelism
}
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.progress.models._

/** This flow takes an ingest request, and downloads the entire ZIP file
  * associated with the request to a local (temporary) path.
  *
  * It returns an Either with errors on the Left, or the zipfile and the
  * original request on the Right.
  *
  */
object ZipFileDownloadFlow extends Logging {

  type BagDownload =
    Either[ArchiveError[IngestBagRequest], FileDownloadComplete]

  def apply(snsConfig: SNSConfig)(
    implicit s3Client: AmazonS3,
    snsClient: AmazonSNS,
    parallelism: Parallelism
  ): Flow[IngestBagRequest, BagDownload, NotUsed] = {

    val materializerType = "akka.stream.materializer.blocking-io-dispatcher"
    val actorAttributes = ActorAttributes.dispatcher(materializerType)
    val downloadSuccessMessage = "Ingest bag file downloaded successfully."

    val snsPublishFlow = SnsPublishFlow[ProgressUpdate](
      snsClient,
      snsConfig,
      subject = "archivist_progress"
    )

    Flow[IngestBagRequest]
      .flatMapMerge(
        parallelism.value, { request =>
          val ingestJob = request.toIngestBagJob

          val updates = ingestJob.bagDownload.fold(
            error => ProgressUpdate.failed(request.id, error.exception),
            _ => ProgressUpdate.event(request.id, downloadSuccessMessage)
          )

          Source
            .single(updates)
            .via(snsPublishFlow)
            .map(_ => ingestJob.bagDownload)
            .withAttributes(actorAttributes)
        }
      )
  }
}
