package uk.ac.wellcome.platform.archive.progress_async.flows

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.CallbackNotification
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.json.JsonUtil._

object CallbackNotificationFlow extends Logging {
  import CallbackNotification._

  def apply(snsClient: AmazonSNS,
            snsConfig: SNSConfig): Flow[Progress, Unit, NotUsed] = {
    val publishFlow = SnsPublishFlow[CallbackNotification](
      snsClient,
      snsConfig,
      Some("callback_notification")
    )

    Flow[Progress].flatMapConcat {
      case progress @ Progress(id, _, Some(callbackUri), _, _, _, _) =>
        Source
          .single(CallbackNotification(id, callbackUri, progress))
          .via(publishFlow)
          .map(_ => ())
      case _ => Source.single(())
    }

  }

}
