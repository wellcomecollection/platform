package uk.ac.wellcome.platform.archive.archivist.flow
import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.{NotificationParsingFlow, SnsPublishFlow}
import uk.ac.wellcome.platform.archive.common.models.{IngestBagRequest, NotificationMessage}
import uk.ac.wellcome.platform.archive.common.progress.models.progress.{ProgressEvent, ProgressEventUpdate, ProgressUpdate}

/** Parses a NotificationMessage as an IngestBagRequest, tells the
  * progress service that it's done so, and emits the bag request.
  *
  */
object NotificationMessageFlow extends Logging {
  import IngestBagRequest._

  def apply(parallelism: Int,
            snsClient: AmazonSNS,
            progressSnsConfig: SNSConfig)
    : Flow[NotificationMessage, IngestBagRequest, NotUsed] = {
    Flow[NotificationMessage]
      .via(NotificationParsingFlow[IngestBagRequest])
      .flatMapMerge(
        breadth = parallelism,
        bagRequest => {
          val progressUpdate = ProgressEventUpdate(
            id = bagRequest.archiveRequestId,
            events = List(
              ProgressEvent(
                s"Started working on ingestRequest: ${bagRequest.archiveRequestId}")
            )
          )

          Source
            .single(progressUpdate)
            .via(
              SnsPublishFlow[ProgressUpdate](
                snsClient,
                progressSnsConfig,
                Some("archivist_progress")))
            .map(_ => bagRequest)
        }
      )
  }
}
