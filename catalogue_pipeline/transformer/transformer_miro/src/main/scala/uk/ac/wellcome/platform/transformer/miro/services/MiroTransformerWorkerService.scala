package uk.ac.wellcome.platform.transformer.miro.services

import akka.Done
import uk.ac.wellcome.WorkerService
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.transformer.miro.MiroTransformableTransformer
import uk.ac.wellcome.platform.transformer.miro.models.MiroTransformable
import uk.ac.wellcome.platform.transformer.receive.HybridRecordReceiver

import scala.concurrent.Future

class MiroTransformerWorkerService(
  messageReceiver: HybridRecordReceiver[MiroTransformable],
  miroTransformer: MiroTransformableTransformer,
  sqsStream: SQSStream[NotificationMessage]
) extends WorkerService {

  def run(): Future[Done] =
    sqsStream.foreach(
      this.getClass.getSimpleName,
      (message: NotificationMessage) =>
        messageReceiver.receiveMessage(message, miroTransformer.transform)
    )
}
