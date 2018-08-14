package uk.ac.wellcome.platform.archive.archivist

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNSAsync
import com.google.inject.Injector
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig}
import uk.ac.wellcome.platform.archive.archivist.flow.{
  BagArchiveCompleteFlow,
  DownloadZipFileFlow,
  UploadAndVerifyBagFlow
}
import uk.ac.wellcome.platform.archive.archivist.models.BagUploaderConfig
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait Archivist extends Logging {
  val injector: Injector

  def run() = {
    val messageStream =
      injector.getInstance(classOf[MessageStream[NotificationMessage, Object]])
    val bagUploaderConfig = injector.getInstance(classOf[BagUploaderConfig])

    implicit val s3Client: S3Client = injector.getInstance(classOf[S3Client])
    implicit val snsClient: AmazonSNSAsync =
      injector.getInstance(classOf[AmazonSNSAsync])
    implicit val actorSystem: ActorSystem =
      injector.getInstance(classOf[ActorSystem])
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")

    val snsConfig = injector.getInstance(classOf[SNSConfig])

    val workFlow =
      Flow[NotificationMessage]
        .log("notification message")
        .map(getObjectLocation)
        .log("download location")
        .via(DownloadZipFileFlow())
        .log("download zip")
        .via(UploadAndVerifyBagFlow(bagUploaderConfig))
        .log("archive verified")
        .via(BagArchiveCompleteFlow(snsConfig.topicArn))

    messageStream.run("archivist", workFlow)
  }

  private def getObjectLocation(message: NotificationMessage) = {
    fromJson[ObjectLocation](message.Message) match {
      case Success(location) => location
      case Failure(e) =>
        throw new RuntimeException(
          s"Failed to get object location from notification: ${e.getMessage}"
        )
    }
  }
}
