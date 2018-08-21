package uk.ac.wellcome.platform.transformer.miro.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.transformer.miro.receive.MiroNotificationMessageReceiver

import scala.concurrent.Future

class MiroTransformerWorkerService @Inject()(
                                          system: ActorSystem,
                                          messageReceiver: MiroNotificationMessageReceiver,
                                          sqsStream: SQSStream[NotificationMessage]
) {

  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    messageReceiver.receiveMessage(message)

  def stop() = system.terminate()
}
