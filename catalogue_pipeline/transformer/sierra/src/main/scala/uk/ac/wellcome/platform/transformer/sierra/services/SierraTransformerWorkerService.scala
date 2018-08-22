package uk.ac.wellcome.platform.transformer.sierra.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.platform.transformer.receive.HybridRecordReceiver
import uk.ac.wellcome.platform.transformer.sierra.SierraTransformableTransformer

import scala.concurrent.Future

class SierraTransformerWorkerService @Inject()(
                                                system: ActorSystem,
                                                messageReceiver: HybridRecordReceiver[SierraTransformable],
                                                sierraTransformer: SierraTransformableTransformer,
                                                sqsStream: SQSStream[NotificationMessage]
) {

  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    messageReceiver.receiveMessage(message, sierraTransformer.transform)

  def stop() = system.terminate()
}
