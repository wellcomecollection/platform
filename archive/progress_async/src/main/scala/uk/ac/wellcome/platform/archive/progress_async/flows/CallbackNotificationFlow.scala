package uk.ac.wellcome.platform.archive.progress_async.flows

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishResult
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.flows.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.CallbackNotification
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.json.JsonUtil._

object CallbackNotificationFlow {
  def apply(snsClient: AmazonSNS, snsConfig: SNSConfig): Flow[Progress, PublishResult, NotUsed] = {
    val publishFlow = SnsPublishFlow[CallbackNotification](
      snsClient,
      snsConfig,
      "callback_notification"
    )

    Flow[Progress]
      .map(CallbackNotification(_))
      .via(publishFlow)
  }
}
