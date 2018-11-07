package uk.ac.wellcome.platform.archive.progress_async

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNS
import com.google.inject.{Guice, Injector}
import com.typesafe.config.ConfigFactory
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.config.builders.{AkkaBuilder, MessagingBuilder, MetricsBuilder, SQSBuilder}
import uk.ac.wellcome.platform.archive.common.messaging.{MessageStream, NotificationParsingFlow}
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.platform.archive.common.progress.models.ProgressUpdate
import uk.ac.wellcome.platform.archive.common.progress.modules.ProgressTrackerModule
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.platform.archive.progress_async.flows.{CallbackNotificationFlow, ProgressUpdateFlow}
import uk.ac.wellcome.platform.archive.progress_async.modules.{AppConfigModule, ConfigModule}

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App with Logging {
  val injector: Injector = Guice.createInjector(
    new AppConfigModule(args),
    ConfigModule,
    AkkaModule,
    CloudWatchClientModule,
    SQSClientModule,
    SNSClientModule,
    ProgressTrackerModule,
    MessageStreamModule
  )

  val actorSystem = AkkaBuilder.buildActorSystem()

  val config = ConfigFactory.load()

  val messageStream = MessagingBuilder.buildMessageStream[NotificationMessage, Unit](config)

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



  try {
    info(s"Starting worker.")

    val app = run()

    Await.result(app, Duration.Inf)
  } catch {
    case e: Throwable =>
      error("Fatal error:", e)
  } finally {
    info(s"Terminating worker.")
  }
}
