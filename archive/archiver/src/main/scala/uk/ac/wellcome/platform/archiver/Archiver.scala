package uk.ac.wellcome.platform.archiver

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Flow
import com.google.inject.Injector
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.archiver.flow.{
  BagLocationFromNotificationFlow,
  DownloadZipFileFlow,
  UploadAndVerifyBagFlow
}
import uk.ac.wellcome.platform.archiver.messaging.MessageStream
import uk.ac.wellcome.platform.archiver.models.BagUploaderConfig
import uk.ac.wellcome.json.JsonUtil._

trait Archiver extends Logging {
  val injector: Injector

  def run() = {
    val messageStream =
      injector.getInstance(classOf[MessageStream[NotificationMessage, Object]])
    val bagUploaderConfig = injector.getInstance(classOf[BagUploaderConfig])

    implicit val s3Client: S3Client = injector.getInstance(classOf[S3Client])
    implicit val actorSystem: ActorSystem =
      injector.getInstance(classOf[ActorSystem])
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")

    val workFlow = Flow[NotificationMessage]
      .log("notification")
      .via(BagLocationFromNotificationFlow())
      .log("download notice")
      .via(DownloadZipFileFlow())
      .log("download zip")
      .via(UploadAndVerifyBagFlow(bagUploaderConfig))
      .log("archive verified")

    messageStream.run("archiver", workFlow)
  }
}
