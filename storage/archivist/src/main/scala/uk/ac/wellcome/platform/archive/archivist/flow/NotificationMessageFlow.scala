package uk.ac.wellcome.platform.archive.archivist.flow

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.sns.AmazonSNS
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig}
import uk.ac.wellcome.platform.archive.common.config.models.Parallelism
import uk.ac.wellcome.platform.archive.common.messaging.{
  NotificationParsingFlow,
  SnsPublishFlow
}
import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest
import uk.ac.wellcome.platform.archive.common.progress.models.{
  ProgressEvent,
  ProgressEventUpdate,
  ProgressUpdate
}

/** Parses a NotificationMessage as an IngestBagRequest, tells the
  * progress service that it's done so, and emits the bag request.
  *
  */
object NotificationMessageFlow {
  def apply(progressSnsConfig: SNSConfig)(
    implicit snsClient: AmazonSNS,
    parallelism: Parallelism
  ): Flow[NotificationMessage, IngestBagRequest, NotUsed] = {
    Flow[NotificationMessage]
      .via(NotificationParsingFlow[IngestBagRequest])
      .flatMapMerge(
        breadth = parallelism.value,
        bagRequest => {
          val progressUpdate = ProgressEventUpdate(
            id = bagRequest.id,
            events = List(
              ProgressEvent("Started work on ingest")
            )
          )

          Source
            .single(progressUpdate)
            .via(
              SnsPublishFlow[ProgressUpdate](
                snsClient,
                progressSnsConfig,
                subject = "archivist_progress"))
            .map(_ => bagRequest)
        }
      )
  }
}
