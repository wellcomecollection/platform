package uk.ac.wellcome.platform.archive.notifier

import java.net.URL

import akka.Done
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishResult
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig}
import uk.ac.wellcome.platform.archive.common.messaging.{
  MessageStream,
  NotificationParsingFlow
}
import uk.ac.wellcome.platform.archive.common.models.CallbackNotification
import uk.ac.wellcome.platform.archive.notifier.flows.NotificationFlow
import uk.ac.wellcome.typesafe.Runnable

import scala.concurrent.Future

class Notifier(
  messageStream: MessageStream[NotificationMessage, PublishResult],
  snsClient: AmazonSNS,
  snsConfig: SNSConfig,
  contextUrl: URL
)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer)
    extends Runnable {
  def run(): Future[Done] = {
    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")

    val notificationParsingFlow =
      NotificationParsingFlow[CallbackNotification]()

    val workflow = NotificationFlow(
      contextUrl = contextUrl,
      snsClient = snsClient,
      snsConfig = snsConfig
    )

    val flow = notificationParsingFlow.via(workflow)

    messageStream.run("notifier", flow)
  }
}
