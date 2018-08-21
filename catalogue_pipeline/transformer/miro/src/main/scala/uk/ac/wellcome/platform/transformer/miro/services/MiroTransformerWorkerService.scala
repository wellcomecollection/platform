package uk.ac.wellcome.platform.transformer.miro.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.message.MessageStream
import uk.ac.wellcome.models.transformable.MiroTransformable
import uk.ac.wellcome.platform.transformer.miro.receive.MiroTransformableReceiver

import scala.concurrent.Future

class MiroTransformerWorkerService @Inject()(
                                              system: ActorSystem,
                                              messageReceiver: MiroTransformableReceiver,
                                              sqsStream: MessageStream[MiroTransformable]
) {

  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: MiroTransformable): Future[Unit] =
    messageReceiver.receiveMessage(message)

  def stop() = system.terminate()
}
