package uk.ac.wellcome.metrics

import java.util.Date

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model._
import com.google.inject.Inject
import com.twitter.inject.annotations.Flag

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class MetricsSender @Inject()(@Flag("aws.metrics.namespace") namespace: String,
                              amazonCloudWatch: AmazonCloudWatch) {

  def timeAndCount[T](metricName: String, fun: () => Future[T]): Future[T] = {
    val start = new Date()
    val future = Future.successful(()).flatMap(fun(_))
    future.onComplete {
      case Success(_) =>
        val end = new Date()
        incrementCount("success")
        sendTime(metricName,
          (end.getTime - start.getTime) milliseconds,
          Map("success" -> "true"))

      case Failure(_) =>
        val end = new Date()
        incrementCount("failures")
        sendTime("ingest-time",
          (end.getTime - start.getTime) milliseconds,
          Map("success" -> "true"))
    }
    future
  }

  def incrementCount(metricName: String): PutMetricDataResult = {
    amazonCloudWatch.putMetricData(
      new PutMetricDataRequest()
        .withNamespace(namespace)
        .withMetricData(
          new MetricDatum()
            .withMetricName(metricName)
            .withValue(1.0)
            .withUnit(StandardUnit.Count)
            .withTimestamp(new Date())))
  }

  def sendTime(
    metricName: String,
    time: Duration,
    dimensions: Map[String, String] = Map()): PutMetricDataResult = {
    amazonCloudWatch.putMetricData(
      new PutMetricDataRequest()
        .withNamespace(namespace)
        .withMetricData(
          new MetricDatum()
            .withMetricName(metricName)
            .withDimensions(
              dimensions.foldLeft(Nil: List[Dimension])((acc, pair) => {
                val dimension =
                  new Dimension().withName(pair._1).withValue(pair._2)
                dimension :: acc
              }))
            .withValue(time.toMillis.toDouble)
            .withUnit(StandardUnit.Milliseconds)
            .withTimestamp(new Date())))
  }
}
