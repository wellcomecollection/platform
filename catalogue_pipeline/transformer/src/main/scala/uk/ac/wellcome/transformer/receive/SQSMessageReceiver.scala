package uk.ac.wellcome.transformer.receive

import com.twitter.inject.Logging
import io.circe.ParsingFailure
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.transformer.parsers.TransformableParser
import uk.ac.wellcome.transformer.transformers.TransformableTransformer

import scala.concurrent.Future
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.util.{Failure, Success, Try}

class SQSMessageReceiver(
  snsWriter: SNSWriter,
  transformableParser: TransformableParser,
  transformableTransformer: TransformableTransformer[Transformable],
  metricsSender: MetricsSender)
    extends Logging {

  def receiveMessage(message: SQSMessage): Future[Unit] = {
    info(s"Starting to process message $message")
    metricsSender.timeAndCount(
      "ingest-time",
      () => {
        val triedWork = for {
          transformableRecord <- transformableParser.extractTransformable(
            message.body)
          cleanRecord <- transformTransformable(transformableRecord)
        } yield cleanRecord

        triedWork match {
          case Success(Some(work)) => publishMessage(work).map(_ => ())
          case Success(None) => Future.successful()
          case Failure(e: ParsingFailure) =>
            info("Recoverable failure extracting workfrom record", e)
            Future.failed(GracefulFailureException(e))
          case Failure(e) =>
            info("Unrecoverable failure extracting work from record", e)
            Future.failed(e)
        }
      }
    )
  }

  def transformTransformable(transformable: Transformable): Try[Option[Work]] = {
    transformableTransformer.transform(transformable) map { transformed =>
      info(s"Transformed record $transformed")
      transformed
    } recover {
      case e: Throwable =>
        error("Failed to perform transform to unified item", e)
        throw e
    }
  }

  def publishMessage(work: Work): Future[PublishAttempt] =
    snsWriter.writeMessage(JsonUtil.toJson(work).get, Some("Foo"))
}
