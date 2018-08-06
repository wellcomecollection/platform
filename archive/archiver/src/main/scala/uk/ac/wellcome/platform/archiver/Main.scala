package uk.ac.wellcome.platform.archiver

import java.util.zip.ZipFile

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Flow
import akka.stream.{ActorMaterializer, Attributes}
import akka.{Done, NotUsed}
import com.google.inject.{Guice, Injector}
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.archiver.flow.{
  DownloadNotificationFlow,
  DownloadZipFlow,
  VerifiedBagUploaderFlow
}
import uk.ac.wellcome.platform.archiver.messaging.MessageStream
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.platform.archiver.modules._
import uk.ac.wellcome.storage.ObjectLocation

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
      injector.getInstance(classOf[MessageStream[NotificationMessage, Unit]])
    val s3Client = injector.getInstance(classOf[S3Client])
    val actorSystem = injector.getInstance(classOf[ActorSystem])
    val bagUploaderConfig = injector.getInstance(classOf[BagUploaderConfig])
    val materializer = ActorMaterializer()(actorSystem)

    val downloadNotificationFlow
      : Flow[NotificationMessage, ObjectLocation, NotUsed] =
      DownloadNotificationFlow()
    val downloadZipFlow: Flow[ObjectLocation, ZipFile, NotUsed] =
      DownloadZipFlow(s3Client, materializer, actorSystem.dispatcher)
    val verifiedBagUploaderFlow: Flow[ZipFile, Seq[Done], NotUsed] =
      VerifiedBagUploaderFlow(bagUploaderConfig)(materializer, s3Client)

    val workFlow = Flow[NotificationMessage]
      .log("notification")
      .via(downloadNotificationFlow)
      .log("download notice")
      .via(downloadZipFlow)
      .log("download zip")
      .via(verifiedBagUploaderFlow)
      .log("archive verified")
      .map(_ => ())
      .withAttributes(
        Attributes.logLevels(
          onElement = Logging.WarningLevel,
          onFinish = Logging.InfoLevel,
          onFailure = Logging.DebugLevel
        ))

    messageStream.run("archiver", workFlow)
  }
}
