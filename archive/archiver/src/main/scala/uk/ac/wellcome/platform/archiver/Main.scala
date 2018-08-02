package uk.ac.wellcome.platform.archiver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, Source}
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
    val messageStream = injector.getInstance(classOf[MessageStream[NotificationMessage, Unit]])
    val s3Client = injector.getInstance(classOf[S3Client])
    val actorSystem = injector.getInstance(classOf[ActorSystem])
    val bagUploaderConfig = injector.getInstance(classOf[BagUploaderConfig])
    val materializer = ActorMaterializer()(actorSystem)

    val workFlow = Flow[NotificationMessage]
      .via(DownloadNotificationFlow())
      .via(DownloadZipFlow(s3Client, materializer, actorSystem.dispatcher))
      .flatMapConcat(zipFile => {
        VerifiedBagUploaderFlow(bagUploaderConfig, zipFile)(materializer, s3Client, actorSystem.dispatcher)
      })
      .flatMapConcat(futureDone => {
        Source.fromFuture(futureDone)
      })
      .map(_ => ())

    messageStream.run("archiver", workFlow)
  }
}