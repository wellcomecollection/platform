package uk.ac.wellcome.metrics

import java.util.Date

import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model._
import com.google.inject.Inject
import com.twitter.inject.annotations.Flag
import com.twitter.inject.Logging
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class MetricsSender @Inject()(@Flag("aws.metrics.namespace") namespace: String,
                              amazonCloudWatch: AmazonCloudWatch)
    extends Logging {

  def timeAndCount[T](metricName: String, fun: => Future[T]): Future[T] = {
    val start = new Date()
    val future = Future.successful(()).flatMap(_ => fun)

    future.onComplete {
      case Success(_) =>
        val end = new Date()
        incrementCount("success")
        sendTime(metricName,
                 (end.getTime - start.getTime) milliseconds,
                 Map("success" -> "true"))

      case Failure(_) =>
        val end = new Date()
        incrementCount("failure")
        sendTime(metricName,
                 (end.getTime - start.getTime) milliseconds,
                 Map("success" -> "false"))

    }

    future
  }

  def incrementCount(metricName: String,
                     count: Double = 1.0): Future[PutMetricDataResult] = {
    val f = Future {

      amazonCloudWatch.putMetricData(
        new PutMetricDataRequest()
          .withNamespace(namespace)
          .withMetricData(
            new MetricDatum()
              .withMetricName(metricName)
              .withValue(count)
              .withUnit(StandardUnit.Count)
              .withTimestamp(new Date())))
    }

    f.onFailure {
      case e: Exception => {
        error("Failed to send incrementCount metric!", e)
      }
    }

    f
  }

  def sendTime(
    metricName: String,
    time: Duration,
    dimensions: Map[String, String] = Map()): Future[PutMetricDataResult] = {

    val f = Future {
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

    f.onFailure {
      case e: Exception => {
        error("Failed to send sendTime metric!", e)
      }
    }

    f

  }
}
