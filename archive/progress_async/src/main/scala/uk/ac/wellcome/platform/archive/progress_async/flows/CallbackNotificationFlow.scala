package uk.ac.wellcome.platform.archive.progress_async.flows

import java.net.URI
import java.util.UUID

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.sns.AmazonSNS
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.CallbackNotification
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.progress.models.Callback.Pending
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.common.progress.models.Progress.{
  Completed,
  Failed
}

object CallbackNotificationFlow {
  type Publication = Flow[Progress, Unit, NotUsed]

  def apply(snsClient: AmazonSNS, snsConfig: SNSConfig): Publication = {
    val publishFlow = SnsPublishFlow[CallbackNotification](
      snsClient,
      snsConfig,
      subject = "callback_notification"
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
            _,
            Some(callback),
            progressStatus,
            _,
            _,
            _,
            _) =>
        callback.status match {
          case Pending if List(Completed, Failed) contains progressStatus =>
            notifyFlow(progress, id, callback.uri)
          case _ => Source.single(())
        }
      case _ => Source.single(())
    }
  }
}
