package uk.ac.wellcome.sqs

import com.twitter.inject.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.models.transformable.{
  ShouldNotTransformException,
  Transformable
}
import uk.ac.wellcome.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.sqs.SQSReaderGracefulException
import uk.ac.wellcome.transformer.parsers.TransformableParser
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class SQSMessageReceiver(
  snsWriter: SNSWriter,
  transformableParser: TransformableParser[Transformable],
  metricsSender: MetricsSender)
    extends Logging {

  def receiveMessage(message: SQSMessage): Future[PublishAttempt] = {
    info(s"Starting to process message $message")
    metricsSender.timeAndCount(
      "ingest-time",
      () => {
        val triedWork = for {
          transformableRecord <- transformableParser.extractTransformable(
            message)
          cleanRecord <- transformTransformable(transformableRecord)
        } yield cleanRecord

        triedWork match {
          case Success(work) =>
            publishMessage(work)
          case Failure(SQSReaderGracefulException(e)) =>
            info("Recoverable failure extracting workfrom record", e)
            Future.successful(PublishAttempt(Left(e)))
          case Failure(e) =>
            info("Unrecoverable failure extracting work from record", e)
            Future.failed(e)
        }
      }
    )
  }

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

  def publishMessage(work: Work): Future[PublishAttempt] =
    snsWriter.writeMessage(JsonUtil.toJson(work).get, Some("Foo"))
}
