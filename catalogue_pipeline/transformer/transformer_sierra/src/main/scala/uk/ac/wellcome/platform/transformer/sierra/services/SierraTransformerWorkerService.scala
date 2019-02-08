package uk.ac.wellcome.platform.transformer.sierra.services

import akka.Done
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.platform.transformer.sierra.SierraTransformableTransformer
import uk.ac.wellcome.typesafe.Runnable

import scala.concurrent.Future

class SierraTransformerWorkerService(
  messageReceiver: HybridRecordReceiver[SierraTransformable],
  sierraTransformer: SierraTransformableTransformer,
  sqsStream: SQSStream[NotificationMessage]
) extends Runnable {

  def run(): Future[Done] =
    sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    messageReceiver.receiveMessage(message, sierraTransformer.transform)
}
