package uk.ac.wellcome.transformer.receive

import java.util.Date

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model._
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.models.{Transformable, Work}
import uk.ac.wellcome.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.transformer.parsers.TransformableParser
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class RecordMap(value: java.util.Map[String, AttributeValue])

class RecordReceiver @Inject()(
  snsWriter: SNSWriter,
  transformableParser: TransformableParser[Transformable],
  amazonCloudWatch: AmazonCloudWatch)
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

        amazonCloudWatch.putMetricData(
          new PutMetricDataRequest()
            .withNamespace("transformer")
            .withMetricData(
              new MetricDatum()
                .withMetricName("success")
                .withValue(1.0)
                .withUnit(StandardUnit.Count)
                .withTimestamp(new Date())))

        amazonCloudWatch.putMetricData(
          new PutMetricDataRequest()
            .withNamespace("transformer")
            .withMetricData(
              new MetricDatum()
                .withMetricName("ingest-time")
                .withDimensions(
                  new Dimension().withName("success").withValue("true"))
                .withValue((end.getTime - start.getTime).toDouble)
                .withUnit(StandardUnit.Milliseconds)
                .withTimestamp(new Date())))

      case Failure(exception) =>
        val end = new Date()
        amazonCloudWatch.putMetricData(
          new PutMetricDataRequest()
            .withNamespace("transformer")
            .withMetricData(
              new MetricDatum()
                .withMetricName("failures")
                .withValue(1.0)
                .withUnit(StandardUnit.Count)
                .withTimestamp(new Date())))
//        amazonCloudWatch.putMetricAlarm(
//          new PutMetricAlarmRequest()
//            .withAlarmName("transformer")
//            .withNamespace("failures")
//            .withAlarmName("failure"))
        amazonCloudWatch.putMetricData(
          new PutMetricDataRequest()
            .withNamespace("transformer")
            .withMetricData(
              new MetricDatum()
                .withMetricName("ingest-time")
                .withDimensions(
                  new Dimension().withName("success").withValue("false"))
                .withValue((end.getTime - start.getTime).toDouble)
                .withUnit(StandardUnit.Milliseconds)
                .withTimestamp(new Date())))

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
