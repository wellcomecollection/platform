package uk.ac.wellcome.platform.archive.progress_async

import akka.Done
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig}
import uk.ac.wellcome.platform.archive.common.messaging.{
  MessageStream,
  NotificationParsingFlow
}
import uk.ac.wellcome.platform.archive.common.progress.models.ProgressUpdate
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.platform.archive.progress_async.flows.{
  CallbackNotificationFlow,
  ProgressUpdateFlow
}
import uk.ac.wellcome.typesafe.Runnable

import scala.concurrent.Future

class ProgressAsync(
  messageStream: MessageStream[NotificationMessage, Unit],
  progressTracker: ProgressTracker,
  snsClient: AmazonSNS,
  snsConfig: SNSConfig
)(implicit val actorSystem: ActorSystem, materializer: ActorMaterializer)
    extends Logging
    with Runnable {
  def run(): Future[Done] = {
    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")

    val parseNotificationFlow = NotificationParsingFlow[ProgressUpdate]()

    val progressUpdateFlow = ProgressUpdateFlow(progressTracker)

    val callbackNotificationFlow = CallbackNotificationFlow(
      snsClient = snsClient,
      snsConfig = snsConfig
    )

    val workFlow = Flow[NotificationMessage]
      .log("notification message")
      .via(parseNotificationFlow)
      .via(progressUpdateFlow)
      .via(callbackNotificationFlow)

    messageStream.run("progress", workFlow)
  }
}
