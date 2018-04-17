package uk.ac.wellcome.transformer.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.transformer.receive.SQSMessageReceiver
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import com.amazonaws.services.s3.AmazonS3

import scala.concurrent.Future

class TransformerWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  messageReceiver: SQSMessageReceiver,
  s3: AmazonS3
) extends SQSWorker(reader, system, metrics, s3) {

  override def processMessage(message: SQSMessage): Future[Unit] =
    messageReceiver.receiveMessage(message).map(_ => ())
}
