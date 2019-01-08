package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.archivist.models.TypeAliases._
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.{IngestBagRequest, Parallelism}
import uk.ac.wellcome.platform.archive.common.progress.models._

/** This flow takes an ingest request, and downloads the entire ZIP file
  * associated with the request to a local (temporary) path.
  *
  * It returns an Either with errors on the Left, or the zipfile and the
  * original request on the Right.
  *
  */
object ZipFileDownloadFlow extends Logging {

  def apply(snsConfig: SNSConfig)(
    implicit transferManager: TransferManager,
    snsClient: AmazonSNS,
    parallelism: Parallelism
  ): Flow[IngestBagRequest, BagDownload, NotUsed] = {

    val downloadSuccessMessage = "Ingest bag file downloaded successfully."

    val snsPublishFlow = SnsPublishFlow[ProgressUpdate](
      snsClient,
      snsConfig,
      subject = "archivist_progress"
    )

    Flow[IngestBagRequest].via(S3DownloadFlow(transferManager)).flatMapMerge(
      parallelism.value,
      either => {
        val updates = either.fold(
          error => ProgressUpdate.failed(error.t.id, error),
          download => ProgressUpdate.event(download.ingestBagRequest.id, downloadSuccessMessage)
        )
        Source
          .single(updates)

          .via(snsPublishFlow)
          .map(_ => either)
      }
    )

  }

}
