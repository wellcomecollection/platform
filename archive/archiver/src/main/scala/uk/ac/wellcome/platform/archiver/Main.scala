package uk.ac.wellcome.platform.archiver

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Flow
import com.google.inject.{Guice, Injector}
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.archiver.flow.{DownloadNotificationFlow, DownloadZipFlow, VerifiedBagUploaderFlow}
import uk.ac.wellcome.platform.archiver.messaging.MessageStream
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.platform.archiver.modules._

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App with Archiver {
  override val injector: Injector = Guice.createInjector(
    new AppConfigModule(args),
    AkkaModule,
    AkkaS3ClientModule,
    CloudWatchClientModule,
    SQSClientModule,
    MessageStreamModule
  )

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

trait Archiver extends Logging {
  val injector: Injector

  def run() = {

    val messageStream =
      injector.getInstance(classOf[MessageStream[NotificationMessage, Object]])
    val bagUploaderConfig = injector.getInstance(classOf[BagUploaderConfig])

    implicit val s3Client = injector.getInstance(classOf[S3Client])
    implicit val actorSystem = injector.getInstance(classOf[ActorSystem])
    implicit val materializer = ActorMaterializer()
    implicit val adapter = Logging(actorSystem.eventStream, "customLogger")

    val downloadNotificationFlow = DownloadNotificationFlow()
    val downloadZipFlow = DownloadZipFlow()
    val verifiedBagUploaderFlow = VerifiedBagUploaderFlow(bagUploaderConfig)


    val workFlow = Flow[NotificationMessage]
      .log("notification")
      .via(downloadNotificationFlow)
      .log("download notice")
      .via(downloadZipFlow)
      .log("download zip")
      .via(verifiedBagUploaderFlow)
      .log("archive verified")

    messageStream.run("archiver", workFlow)
  }
}