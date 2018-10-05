package uk.ac.wellcome.platform.archive.progress_async.flows

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishResult
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.CallbackNotification
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.json.JsonUtil._

object CallbackNotificationFlow extends Logging {
  import CallbackNotification._

  def apply(snsClient: AmazonSNS,
            snsConfig: SNSConfig): Flow[Progress, PublishResult, NotUsed] = {
    val publishFlow = SnsPublishFlow[CallbackNotification](
      snsClient,
      snsConfig,
      Some("callback_notification")
    )

    Flow[Progress]
      .map(CallbackNotification(_))
      .via(publishFlow)
  }
}
