package uk.ac.wellcome.platform.goobi_reader.services

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs._
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class GoobiReaderWorkerService @Inject()(
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage]
) extends Logging {

  implicit val actorSystem = system
  implicit val materialiser = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  sqsStream.foreach(
    streamName = this.getClass.getSimpleName,
    process = processMessage
  )

  private def processMessage(notificationMessage: NotificationMessage): Future[Unit] =
    Future { println("I got a message!") }
}
