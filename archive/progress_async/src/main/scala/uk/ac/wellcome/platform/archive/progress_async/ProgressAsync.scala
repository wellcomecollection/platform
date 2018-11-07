package uk.ac.wellcome.platform.archive.progress_async

import akka.Done
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.{MessageStream, NotificationParsingFlow}
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage
import uk.ac.wellcome.platform.archive.common.progress.models.ProgressUpdate
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.platform.archive.progress_async.flows.{CallbackNotificationFlow, ProgressUpdateFlow}

import scala.concurrent.Future

class ProgressAsync(
  messageStream: MessageStream[NotificationMessage, Unit],
  progressTracker: ProgressTracker,
  snsClient: AmazonSNS,
  snsConfig: SNSConfig
) extends Logging {
  def run(): Future[Done] = {
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