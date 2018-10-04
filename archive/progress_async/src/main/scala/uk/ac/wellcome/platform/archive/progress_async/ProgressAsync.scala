package uk.ac.wellcome.platform.archive.progress_async

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNSAsync
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
import uk.ac.wellcome.platform.archive.common.progress.models.ProgressUpdate
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor
import uk.ac.wellcome.platform.archive.progress_async.flows.CallbackNotificationFlow

trait ProgressAsync extends Logging {
  val injector: Injector

  def run() = {

    type StreamNotice = MessageStream[NotificationMessage, Object]

    implicit val snsClient: AmazonSNSAsync =
      injector.getInstance(classOf[AmazonSNSAsync])
    val snsConfig = injector.getInstance(classOf[SNSConfig])

    implicit val system =
      injector.getInstance(classOf[ActorSystem])

    implicit val materializer = ActorMaterializer()

    implicit val adapter =
      Logging(system.eventStream, "customLogger")

    val messageStream =
      injector.getInstance(classOf[StreamNotice])

    val progressMonitor = injector
      .getInstance(classOf[ProgressMonitor])

    val progressUpdateFlow =
      ProgressUpdateFlow(progressMonitor)

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
