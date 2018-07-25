package uk.ac.wellcome.monitoring

import java.util.Date

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, Sink, Source, SourceQueueWithComplete}
import akka.stream.{
  ActorMaterializer,
  OverflowStrategy,
  QueueOfferResult,
  ThrottleMode
}
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model._
import com.google.inject.Inject
import grizzled.slf4j.Logging
import uk.ac.wellcome.exceptions.GracefulFailureException

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class MetricsSender @Inject()(amazonCloudWatch: AmazonCloudWatch,
                              actorSystem: ActorSystem,
                              metricsConfig: MetricsConfig)
    extends Logging {

  implicit val system = actorSystem
  implicit val materialiser = ActorMaterializer()

  // According to https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_limits.html
  // PutMetricData supports a maximum of 20 MetricDatum per PutMetricDataRequest.
  // The maximum number of PutMetricData requests is 150 per second.
  private val metricDataListMaxSize = 20
  private val maxPutMetricDataRequestsPerSecond = 150

  val sink: Sink[Seq[MetricDatum], Future[Done]] = Sink.foreach(
    metricDataSeq =>
      amazonCloudWatch.putMetricData(
        new PutMetricDataRequest()
          .withNamespace(metricsConfig.namespace)
          .withMetricData(metricDataSeq: _*)
    ))

  val source: Source[MetricDatum, SourceQueueWithComplete[MetricDatum]] =
    Source
      .queue[MetricDatum](5000, OverflowStrategy.backpressure)

  val materializer = Flow[MetricDatum]
    .groupedWithin(metricDataListMaxSize, metricsConfig.flushInterval)

  val sourceQueue: SourceQueueWithComplete[MetricDatum] =
    source
      .viaMat(materializer)(Keep.left)
      // Make sure we don't exceed aws rate limit
      .throttle(
        maxPutMetricDataRequestsPerSecond,
        1 second,
        0,
        ThrottleMode.shaping)
      .to(sink)
      .run()

  def count[T](metricName: String, f: Future[T])(
    implicit ec: ExecutionContext): Future[T] = {
    f.onComplete {
      case Success(_) => incrementCount(s"${metricName}_success")
      case Failure(_: GracefulFailureException) =>
        incrementCount(s"${metricName}_gracefulFailure")
      case Failure(_) => incrementCount(s"${metricName}_failure")
    }
    f
  }

  def incrementCount(metricName: String): Future[QueueOfferResult] = {
    val metricDatum = new MetricDatum()
      .withMetricName(metricName)
      .withValue(1.0)
      .withUnit(StandardUnit.Count)
      .withTimestamp(new Date())

    sendToStream(metricDatum)
  }

  private def sendToStream(
    metricDatum: MetricDatum
  ): Future[QueueOfferResult] =
    sourceQueue.offer(metricDatum)
}
