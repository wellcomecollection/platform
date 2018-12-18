package uk.ac.wellcome.platform.transformer.miro.services

import akka.Done
import uk.ac.wellcome.Runnable
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.transformer.miro.MiroTransformableTransformer

import scala.concurrent.Future

class MiroTransformerWorkerService(
  vhsRecordReceiver: MiroVHSRecordReceiver,
  miroTransformer: MiroTransformableTransformer,
  sqsStream: SQSStream[NotificationMessage]
) extends Runnable {

  def run(): Future[Done] =
    sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    vhsRecordReceiver.receiveMessage(message, miroTransformer.transform)
}
