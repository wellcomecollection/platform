package uk.ac.wellcome.platform.archive.progress_async.flows

import java.net.URI
import java.util.UUID

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.CallbackNotification
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.progress.models.Progress.{
  Completed,
  Failed
}

object CallbackNotificationFlow extends Logging {
  import CallbackNotification._

  type Publication = Flow[Progress, Unit, NotUsed]

  def apply(snsClient: AmazonSNS, snsConfig: SNSConfig): Publication = {

    val publishFlow = SnsPublishFlow[CallbackNotification](
      snsClient,
      snsConfig,
      Some("callback_notification")
    )

    def notifyFlow(progress: Progress, id: UUID, callbackUri: URI) = {
      val notification = CallbackNotification(id, callbackUri, progress)
      Source
        .single(notification)
        .via(publishFlow)
        .map(_ => ())
    }

    Flow[Progress].flatMapConcat {
      case progress @ Progress(
            id,
            _,
            Some(callbackUri),
            _,
            Completed,
            _,
            _,
            _) =>
        notifyFlow(progress, id, callbackUri)
      case progress @ Progress(id, _, Some(callbackUri), _, Failed, _, _, _) =>
        notifyFlow(progress, id, callbackUri)
      case _ => Source.single(())
    }

  }

}
