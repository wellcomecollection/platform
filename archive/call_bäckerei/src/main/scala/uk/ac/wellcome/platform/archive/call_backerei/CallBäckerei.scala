package uk.ac.wellcome.platform.archive.call_backerei

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sns.AmazonSNSAsync
import com.google.inject._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.call_backerei.flows.CallbackFlow
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.platform.archive.common.models.NotificationMessage
import uk.ac.wellcome.platform.archive.common.progress.models.Progress

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

class CallBäckerei @Inject()(
  snsClient: AmazonSNSAsync,
  snsConfig: SNSConfig,
  messageStream: MessageStream[NotificationMessage, Object],
  actorSystem: ActorSystem
) {
  def run() = {

    implicit val client = snsClient
    implicit val system = actorSystem

    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")

    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor =
      actorSystem.dispatcher

    val workFlow = Flow[NotificationMessage]
      .map(parseNotification)
      .via(CallbackFlow())
      .log("executed callback")

    messageStream.run("callBäckerei", workFlow)
  }

  private def parseNotification(message: NotificationMessage) = {
    fromJson[Progress](message.Message) match {
      case Success(progress: Progress) => progress
      case Failure(e) =>
        throw new RuntimeException(
          s"Failed to get Progress from notification: ${e.getMessage}"
        )
    }
  }
}
