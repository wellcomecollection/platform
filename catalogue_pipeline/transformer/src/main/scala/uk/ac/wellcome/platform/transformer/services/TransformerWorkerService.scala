package uk.ac.wellcome.platform.transformer.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.transformer.receive.NotificationMessageReceiver
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

class TransformerWorkerService @Inject()(
  system: ActorSystem,
  messageReceiver: NotificationMessageReceiver,
  sqsStream: SQSStream[NotificationMessage]
)(implicit ec: ExecutionContext) {

  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    messageReceiver.receiveMessage(message).map(_ => ())

  def stop() = system.terminate()
}
