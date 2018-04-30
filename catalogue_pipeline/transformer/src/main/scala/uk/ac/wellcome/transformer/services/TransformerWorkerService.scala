package uk.ac.wellcome.transformer.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sqs.{SQSMessage, SQSReader, SQSWorker}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.transformer.receive.SQSMessageReceiver

import scala.concurrent.Future

class TransformerWorkerService @Inject()(
  reader: SQSReader,
  system: ActorSystem,
  metrics: MetricsSender,
  messageReceiver: SQSMessageReceiver
) extends SQSWorker(reader, system, metrics) {

  override def processMessage(message: SQSMessage): Future[Unit] =
    messageReceiver.receiveMessage(message).map(_ => ())
}
