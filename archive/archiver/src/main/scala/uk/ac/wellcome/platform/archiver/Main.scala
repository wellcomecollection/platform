package uk.ac.wellcome.platform.archiver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{Flow, Source}
import com.google.inject.{Guice, Injector}
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.archiver.flow.{DownloadNotificationFlow, DownloadZipFlow, VerifiedBagUploaderFlow}
import uk.ac.wellcome.platform.archiver.messaging.MessageStream
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.platform.archiver.modules._
import uk.ac.wellcome.utils.JsonUtil._

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

  run()
}

trait Archiver extends Logging {
  val injector: Injector

  def run() = {
    try {
      info(s"Starting worker.")

      val messageStream = injector.getInstance(classOf[MessageStream[NotificationMessage, Unit]])
      val s3Client = injector.getInstance(classOf[S3Client])
      val actorSystem = injector.getInstance(classOf[ActorSystem])
      val bagUploaderConfig = injector.getInstance(classOf[BagUploaderConfig])

      val materializer = ActorMaterializer()(actorSystem)

      val workFlow = Flow[NotificationMessage]
        .via(DownloadNotificationFlow())
        .via(DownloadZipFlow(s3Client, materializer, actorSystem.dispatcher))
        .flatMapConcat(zipFile => {
          VerifiedBagUploaderFlow(bagUploaderConfig, zipFile, "gabname")(materializer, s3Client)
        })
        .flatMapConcat(futureDone => {
          Source.fromFuture(futureDone)
        })
        .map(_ => ())

      val done = messageStream.run("archiver", workFlow)
      Await.result(done, Duration.Inf)

    } finally {
      info(s"Terminating worker.")
    }
  }
}