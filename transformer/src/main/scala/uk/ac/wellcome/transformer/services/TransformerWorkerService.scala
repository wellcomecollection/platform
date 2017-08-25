package uk.ac.wellcome.transformer.services

import akka.actor.ActorSystem
import com.google.inject.Inject

import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.transformer.parsers.TransformableParser
import uk.ac.wellcome.transformer.SQSMessageReceiverBuilder
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

class TransformerWorkerService @Inject()(
  reader: SQSReader,
  writer: SNSWriter,
  system: ActorSystem,
  metrics: MetricsSender,
  transformableParser: TransformableParser[Transformable]
) extends SQSWorker(reader, system, metrics) {

  private val messageReceiver = SQSMessageReceiverBuilder.buildReceiver(
    snsWriter = writer,
    parser = transformableParser,
    metricsSender = metrics
  )

  override def processMessage(message: SQSMessage): Future[Unit] =
    messageReceiver.receiveMessage(message).map(_ => ())
}
