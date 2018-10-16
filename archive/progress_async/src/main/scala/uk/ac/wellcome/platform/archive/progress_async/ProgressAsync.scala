package uk.ac.wellcome.platform.archive.progress_async

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNS
import com.google.inject.Injector
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.{
  MessageStream,
  NotificationParsingFlow
}
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage
import uk.ac.wellcome.platform.archive.common.progress.flows.ProgressUpdateFlow
import uk.ac.wellcome.platform.archive.common.progress.models.progress.ProgressUpdate
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.platform.archive.progress_async.flows.CallbackNotificationFlow

trait ProgressAsync extends Logging {
  val injector: Injector

  def run() = {

    type StreamNotice = MessageStream[NotificationMessage, Unit]

    implicit val snsClient: AmazonSNS =
      injector.getInstance(classOf[AmazonSNS])
    val snsConfig = injector.getInstance(classOf[SNSConfig])

    implicit val system =
      injector.getInstance(classOf[ActorSystem])

    implicit val materializer = ActorMaterializer()

    implicit val adapter =
      Logging(system.eventStream, "customLogger")

    val messageStream =
      injector.getInstance(classOf[StreamNotice])

    val progressTracker = injector
      .getInstance(classOf[ProgressTracker])

    val progressUpdateFlow =
      ProgressUpdateFlow(progressTracker)

    val parseNotificationFlow =
      NotificationParsingFlow[ProgressUpdate]()

    val callbackNotificationFlow =
      CallbackNotificationFlow(snsClient, snsConfig)

    val workFlow = Flow[NotificationMessage]
      .log("notification message")
      .via(parseNotificationFlow)
      .via(progressUpdateFlow)
      .via(callbackNotificationFlow)

    messageStream.run("progress", workFlow)
  }
}
