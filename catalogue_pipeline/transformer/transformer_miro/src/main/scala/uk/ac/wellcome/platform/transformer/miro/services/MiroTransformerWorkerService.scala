package uk.ac.wellcome.platform.transformer.miro.services

import akka.Done
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.transformer.miro.MiroTransformableTransformer
import uk.ac.wellcome.platform.transformer.miro.models.MiroTransformable
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord
import uk.ac.wellcome.platform.transformer.receive.HybridRecordReceiver

import scala.concurrent.Future
import scala.util.Try

class MiroTransformerWorkerService(
  messageReceiver: HybridRecordReceiver[MiroTransformable],
  miroTransformer: MiroTransformableTransformer,
  sqsStream: SQSStream[NotificationMessage]
) {

  def run(): Future[Done] =
    sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    messageReceiver.receiveMessage(message, transform)

  private def transform(
    transformable: MiroTransformable,
    version: Int
  ): Try[TransformedBaseWork] =
    miroTransformer.transform(
      miroRecord = MiroRecord.create(transformable.data),
      version = version
    )
}
