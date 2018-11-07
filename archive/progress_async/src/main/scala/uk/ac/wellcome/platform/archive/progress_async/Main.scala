package uk.ac.wellcome.platform.archive.progress_async

import akka.stream.scaladsl.Flow
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.config.builders._
import uk.ac.wellcome.platform.archive.common.messaging.NotificationParsingFlow
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage
import uk.ac.wellcome.platform.archive.common.progress.models.ProgressUpdate
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.platform.archive.progress_async.flows.{CallbackNotificationFlow, ProgressUpdateFlow}

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App with Logging {
  val config = ConfigFactory.load()

  val messageStream = MessagingBuilder.buildMessageStream[NotificationMessage, Unit](config)

  val parseNotificationFlow = NotificationParsingFlow[ProgressUpdate]()

  val progressTracker = new ProgressTracker(
    dynamoClient = DynamoBuilder.buildDynamoClient(config),
    dynamoConfig = DynamoBuilder.buildDynamoConfig(config)
  )

  val progressUpdateFlow = ProgressUpdateFlow(progressTracker)

  val callbackNotificationFlow = CallbackNotificationFlow(
    snsClient = SNSBuilder.buildSNSClient(config),
    snsConfig = SNSBuilder.buildSNSConfig(config)
  )

  val workFlow = Flow[NotificationMessage]
    .log("notification message")
    .via(parseNotificationFlow)
    .via(progressUpdateFlow)
    .via(callbackNotificationFlow)

  try {
    info(s"Starting worker.")

    val app = messageStream.run("progress", workFlow)

    Await.result(app, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating worker.")
  }
}
