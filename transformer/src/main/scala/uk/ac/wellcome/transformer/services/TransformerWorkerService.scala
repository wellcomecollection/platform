package uk.ac.wellcome.transformer.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.Transformable
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.transformer.parsers.TransformableParser
import uk.ac.wellcome.transformer.receive.SQSMessageReceiver
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class TransformerWorkerService @Inject()(
  reader: SQSReader,
  writer: SNSWriter,
  system: ActorSystem,
  metrics: MetricsSender,
  transformableParser: TransformableParser[Transformable]
) extends SQSWorker(reader, system, metrics) {

  private val messageReceiver =
    new SQSMessageReceiver(writer, transformableParser, metrics)

  override def processMessage(message: SQSMessage): Future[Unit] =
    messageReceiver.receiveMessage(message).map(_ => ())
}
