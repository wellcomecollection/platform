package uk.ac.wellcome.platform.archive.progress

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.Flow
import com.google.inject.Injector
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage
import uk.ac.wellcome.platform.archive.common.progress.models.ProgressUpdate

import scala.util.{Failure, Success}

trait Progress extends Logging {
  val injector: Injector

  def run() = {
    //    implicit val s3Client: S3Client = injector.getInstance(classOf[S3Client])
    //    implicit val snsClient: AmazonSNSAsync =
    //      injector.getInstance(classOf[AmazonSNSAsync])
    //    val snsConfig = injector.getInstance(classOf[SNSConfig])

    implicit val system =
      injector.getInstance(classOf[ActorSystem])

    //    implicit val materializer =
    //      ActorMaterializer()

    implicit val adapter =
      Logging(system.eventStream, "customLogger")

    val messageStream =
      injector.getInstance(classOf[MessageStream[NotificationMessage, Object]])

    val workFlow =
      Flow[NotificationMessage]
        .log("notification message")
        .map(parseNotification)

    messageStream.run("archivist", workFlow)
  }

  private def parseNotification(message: NotificationMessage) = {
    fromJson[ProgressUpdate](message.Message) match {
      case Success(progressUpdate) => progressUpdate
      case Failure(e) =>
        throw new RuntimeException(
          s"Failed to get progressUpdate from notification: ${e.getMessage}"
        )
    }
  }
}

