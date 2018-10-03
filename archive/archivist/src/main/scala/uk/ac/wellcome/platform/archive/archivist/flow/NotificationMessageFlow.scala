package uk.ac.wellcome.platform.archive.archivist.flow
import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil.fromJson
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.flows.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.{IngestBagRequest, NotificationMessage}
import uk.ac.wellcome.platform.archive.common.progress.models.{ProgressEvent, ProgressUpdate}
import uk.ac.wellcome.json.JsonUtil._

import scala.util.{Failure, Success}

object NotificationMessageFlow extends Logging{
  def apply(parallelism: Int, snsClient: AmazonSNS, progressSnsConfig: SNSConfig): Flow[NotificationMessage, IngestBagRequest, NotUsed] = {
    Flow[NotificationMessage]
      .map(message => fromJson[IngestBagRequest](message.Message))
      .filter{
        case Success(_) => true
        case Failure(ex) =>
          error("Failed parsing message", ex)
        false
      }
      .collect {
        case Success(bagRequest) => bagRequest
      }
      .flatMapMerge(parallelism, bagRequest => {
        Source.single(ProgressUpdate(bagRequest.archiveRequestId, List(ProgressEvent(s"Started working on ingestRequest: ${bagRequest.archiveRequestId}"))))
          .via(SnsPublishFlow(snsClient, progressSnsConfig)).map(_ => bagRequest)
      })
  }
}
