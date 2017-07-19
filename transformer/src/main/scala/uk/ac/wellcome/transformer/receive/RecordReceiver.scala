package uk.ac.wellcome.transformer.receive

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.{Transformable, Work}
import uk.ac.wellcome.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.transformer.parsers.TransformableParser
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class RecordMap(value: java.util.Map[String, AttributeValue])

class RecordReceiver @Inject()(
  snsWriter: SNSWriter,
  transformableParser: TransformableParser[Transformable],
  metricsSender: MetricsSender)
    extends Logging {

  def receiveRecord(message: SQSMessage): Future[PublishAttempt] = {
    info(s"Starting to process message $message")
    metricsSender.timeAndCount(
      "ingest-time",
      () => {
        val triedWork = for {
          transformableRecord <- transformableParser.extractTransformable(
            message)
          cleanRecord <- transformDynamoRecord(transformableRecord)
        } yield cleanRecord

        triedWork match {
          case Success(work) =>
            publishMessage(work)
          case Failure(e) =>
            error("Failed extracting unified item from record", e)
            Future.failed(e)
        }
      }
    )
  }

  def transformDynamoRecord(transformable: Transformable): Try[Work] = {
    transformable.transform map { transformed =>
      info(s"Transformed record $transformed")
      transformed
    } recover {
      case e: Throwable =>
        // TODO: Send to dead letter queue or just error
        error("Failed to perform transform to unified item", e)
        throw e
    }
  }

  def publishMessage(work: Work): Future[PublishAttempt] =
    snsWriter.writeMessage(JsonUtil.toJson(work).get, Some("Foo"))
}
