package uk.ac.wellcome.platform.archive.archivist.flow
import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.{
  NotificationParsingFlow,
  SnsPublishFlow
}
import uk.ac.wellcome.platform.archive.common.models.{
  IngestBagRequest,
  NotificationMessage
}
import uk.ac.wellcome.platform.archive.common.progress.models.{
  ProgressEvent,
  ProgressUpdate
}

object NotificationMessageFlow extends Logging {
  import IngestBagRequest._

  def apply(parallelism: Int,
            snsClient: AmazonSNS,
            progressSnsConfig: SNSConfig)
    : Flow[NotificationMessage, IngestBagRequest, NotUsed] = {
    Flow[NotificationMessage]
      .via(NotificationParsingFlow[IngestBagRequest])
      .flatMapMerge(
        parallelism,
        bagRequest => {
          Source
            .single(ProgressUpdate(
              bagRequest.archiveRequestId,
              List(ProgressEvent(
                s"Started working on ingestRequest: ${bagRequest.archiveRequestId}"))))
            .via(SnsPublishFlow(snsClient, progressSnsConfig, Some("archivist_progress")))
            .map(_ => bagRequest)
        }
      )
  }
}
