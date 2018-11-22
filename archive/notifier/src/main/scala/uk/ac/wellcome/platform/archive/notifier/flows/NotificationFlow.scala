package uk.ac.wellcome.platform.archive.notifier.flows

import java.net.URL

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishResult
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.SnsPublishFlow
import uk.ac.wellcome.platform.archive.common.models.CallbackNotification
import uk.ac.wellcome.platform.archive.common.progress.models.ProgressUpdate

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
  def apply(contextUrl: URL, snsClient: AmazonSNS, snsConfig: SNSConfig)(
    implicit actorSystem: ActorSystem)
    : Flow[CallbackNotification, PublishResult, NotUsed] = {

    val callbackUrlFlow = CallbackUrlFlow(contextUrl: URL)
    val prepareNotificationFlow = PrepareNotificationFlow()
    val snsPublishFlow = SnsPublishFlow[ProgressUpdate](
      snsClient,
      snsConfig,
      subject = s"Sent by ${this.getClass.getName}"
    )

    callbackUrlFlow
      .via(prepareNotificationFlow)
      .via(snsPublishFlow)
  }
}
