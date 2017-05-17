package uk.ac.wellcome.transformer.receive

import java.util.Date

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model._
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.google.inject.Inject
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.{Transformable, Work}
import uk.ac.wellcome.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.transformer.parsers.TransformableParser
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

case class RecordMap(value: java.util.Map[String, AttributeValue])

class RecordReceiver @Inject()(
  snsWriter: SNSWriter,
  transformableParser: TransformableParser[Transformable],
  metricsSender: MetricsSender)
    extends Logging {

  def receiveRecord(record: RecordAdapter): Future[PublishAttempt] = {
    info(s"Starting to process record $record")
    val start = new Date()
    val triedWork = for {
      recordMap <- recordToRecordMap(record)
      transformableRecord <- transformableParser.extractTransformable(
        recordMap)
      cleanRecord <- transformDynamoRecord(transformableRecord)
    } yield cleanRecord

    val future = triedWork match {
      case Success(work) =>
        publishMessage(work)
      case Failure(e) =>
        error("Failed extracting unified item from record", e)
        Future.failed(e)
    }

    future.onComplete {
      case Success(publishAttempt) =>
        val end = new Date()
        metricsSender.incrementCount("success")
        metricsSender.sendTime("ingest-time", (end.getTime - start.getTime) milliseconds, Map("success"-> "true"))

      case Failure(exception) =>
        val end = new Date()
        metricsSender.incrementCount("failures")
        metricsSender.sendTime("ingest-time", (end.getTime - start.getTime) milliseconds, Map("success"-> "true"))
    }
    future
  }

  def recordToRecordMap(record: RecordAdapter): Try[RecordMap] = Try {
    val keys = record.getInternalObject.getDynamodb.getNewImage

    info(s"Received record $keys")
    RecordMap(keys)
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

