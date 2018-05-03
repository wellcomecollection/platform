package uk.ac.wellcome.monitoring

import java.util.Date

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
import com.twitter.inject.Logging
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class MetricsSender @Inject()(
  @Flag("aws.metrics.namespace") namespace: String,
  @Flag("aws.metrics.flushInterval") flushInterval: FiniteDuration,
  amazonCloudWatch: AmazonCloudWatch,
  actorSystem: ActorSystem)
    extends Logging {
  implicit val system = actorSystem
  implicit val materialiser = ActorMaterializer()

  // According to https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_limits.html
  // PutMetricData supports a maximum of 20 MetricDatum per PutMetricDataRequest.
  // The maximum number of PutMetricData requests is 150 per second.
  private val metricDataListMaxSize = 20
  private val maxPutMetricDataRequestsPerSecond = 150

  val sourceQueue: SourceQueueWithComplete[MetricDatum] =
    Source
      .queue[MetricDatum](5000, OverflowStrategy.backpressure)
      // Group the MetricDatum objects into lists of at max 20 items.
      // Send smaller chunks if not appearing within 10 seconds
      .viaMat(
        Flow[MetricDatum].groupedWithin(metricDataListMaxSize, flushInterval))(
        Keep.left)
      // Make sure we don't exceed aws rate limit
      .throttle(
        maxPutMetricDataRequestsPerSecond,
        1 second,
        0,
        ThrottleMode.shaping)
      .to(
        Sink.foreach(
          metricDataSeq =>
            amazonCloudWatch.putMetricData(
              new PutMetricDataRequest()
                .withNamespace(namespace)
                .withMetricData(metricDataSeq: _*)
          )))
      .run()

  def timeAndCount[T](metricName: String, fun: () => Future[T]): Future[T] = {
    val start = new Date()
    val future = Future.successful(()).flatMap(_ => fun())

    future.onComplete {
      case Success(_) =>
        val end = new Date()
        incrementCount("success")
        sendTime(
          metricName,
          (end.getTime - start.getTime) milliseconds,
          Map("success" -> "true"))

      case Failure(_) =>
        val end = new Date()
        incrementCount("failure")
        sendTime(
          metricName,
          (end.getTime - start.getTime) milliseconds,
          Map("success" -> "false"))

    }

    future
  }

  def incrementCount(metricName: String,
                     count: Double = 1.0): Future[QueueOfferResult] = {

    val metricDatum = new MetricDatum()
      .withMetricName(metricName)
      .withValue(count)
      .withUnit(StandardUnit.Count)
      .withTimestamp(new Date())
    sendToStream(metricDatum)
  }

  def sendTime(
    metricName: String,
    time: Duration,
    dimensions: Map[String, String] = Map()): Future[QueueOfferResult] = {

    val metricDatum = new MetricDatum()
      .withMetricName(metricName)
      .withDimensions(
        dimensions.foldLeft(Nil: List[Dimension])((acc, pair) => {
          val dimension =
            new Dimension().withName(pair._1).withValue(pair._2)
          dimension :: acc
        }))
      .withValue(time.toMillis.toDouble)
      .withUnit(StandardUnit.Milliseconds)
      .withTimestamp(new Date())
    sendToStream(metricDatum)
  }

  private def sendToStream(
    metricDatum: MetricDatum): Future[QueueOfferResult] =
    sourceQueue.offer(metricDatum)
}
