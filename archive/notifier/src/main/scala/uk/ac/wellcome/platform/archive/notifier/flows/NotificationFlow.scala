package uk.ac.wellcome.platform.archive.notifier.flows

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishResult
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.progress.models.ProgressUpdate
import uk.ac.wellcome.platform.archive.notifier.CallbackNotification

/** This flow receives a [[NotificationFlow]] instance, as received from
  * an SQS queue, and makes the callback request (if necessary).
  *
  * After this flow has done, we need to know two things:
  *   - The original Progress object
  *   - Whether the callback succeeded (if necessary) -- we record
  * the callback result.
  *
  */
object NotificationFlow {
  def apply(snsClient: AmazonSNS, snsConfig: SNSConfig)(
    implicit actorSystem: ActorSystem)
    : Flow[CallbackNotification, PublishResult, NotUsed] = {

    val withCallbackUrlFlow = CallbackUrlFlow()
    val prepareNotificationFlow = PrepareNotificationFlow()
    val snsPublishFlow = SnsPublishFlow[ProgressUpdate](snsClient, snsConfig)

    withCallbackUrlFlow
      .via(prepareNotificationFlow)
      .via(snsPublishFlow)
  }
}
