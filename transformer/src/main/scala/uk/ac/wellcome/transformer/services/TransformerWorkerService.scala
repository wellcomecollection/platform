package uk.ac.wellcome.transformer.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.{SQSReader, SQSWorker}
import uk.ac.wellcome.transformer.parsers.TransformableParser
import uk.ac.wellcome.transformer.receive.SQSMessageReceiver
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import uk.ac.wellcome.models.Work
import uk.ac.wellcome.sqs.SQSMessage
import uk.ac.wellcome.sqs.SQSReaderGracefulException
import uk.ac.wellcome.models.transformable.{
  ShouldNotTransformException,
  Transformable
}
import com.twitter.inject.Logging


import scala.concurrent.Future

class TransformerWorkerService @Inject()(
  reader: SQSReader,
  writer: SNSWriter,
  system: ActorSystem,
  metrics: MetricsSender,
  transformableParser: TransformableParser[Transformable]
) extends SQSWorker(reader, system, metrics) with Logging {

  private val messageReceiver = new SQSMessageReceiver(
    snsWriter = writer,
    messageProcessor = messageProcessor,
    metricsSender = metrics
  )

  def messageProcessor(message: SQSMessage): Try[Work] =
    for {
      transformableRecord <- transformableParser.extractTransformable(
        message)
      cleanRecord <- transformTransformable(transformableRecord)
    } yield cleanRecord

  def transformTransformable(transformable: Transformable): Try[Work] = {
    transformable.transform map { transformed =>
      info(s"Transformed record $transformed")
      transformed
    } recover {
      case e: ShouldNotTransformException =>
        info("Work does not meet transform requirements.", e)
        throw SQSReaderGracefulException(e)
      case e: Throwable =>
        // TODO: Send to dead letter queue or just error
        error("Failed to perform transform to unified item", e)
        throw e
    }
  }

  override def processMessage(message: SQSMessage): Future[Unit] =
    messageReceiver.receiveMessage(message).map(_ => ())
}
