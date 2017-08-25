package uk.ac.wellcome.transformer

import scala.util.Try

import com.twitter.inject.Logging

import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.{
  ShouldNotTransformException,
  Transformable
}
import uk.ac.wellcome.sns.SNSWriter
import uk.ac.wellcome.sqs.{SQSMessageReceiver, SQSReaderGracefulException}
import uk.ac.wellcome.transformer.parsers.TransformableParser

object SQSMessageReceiverBuilder extends Logging {

  def buildReceiver(snsWriter: SNSWriter,
                    parser: TransformableParser[Transformable],
                    metricsSender: MetricsSender): SQSMessageReceiver =
    new SQSMessageReceiver(
      snsWriter = snsWriter,
      messageProcessor = buildMessageProcessor(parser),
      metricsSender = metricsSender
    )

  private def buildMessageProcessor(
    parser: TransformableParser[Transformable]): (SQSMessage) => Try[Work] =
    (message: SQSMessage) =>
      for {
        transformableRecord <- parser.extractTransformable(message)
        cleanRecord <- transformTransformable(transformableRecord)
      } yield cleanRecord

  private def transformTransformable(transformable: Transformable): Try[Work] =
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
