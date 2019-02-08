package uk.ac.wellcome.platform.archive.common.http

import akka.http.scaladsl.model.{HttpResponse, StatusCode}
import akka.stream.QueueOfferResult
import grizzled.slf4j.Logging
import uk.ac.wellcome.monitoring.MetricsSender

import scala.concurrent.Future

object HttpMetricResults extends Enumeration {
  type HttpMetricResults = Value
  val Success, UserError, ServerError, Unrecognised = Value
}

class HttpMetrics(name: String, metricsSender: MetricsSender) extends Logging {

  def sendMetric(resp: HttpResponse): Future[QueueOfferResult] =
    sendMetricForStatus(resp.status)

  def sendMetricForStatus(status: StatusCode): Future[QueueOfferResult] = {
    val httpMetric = if (status.isSuccess()) {
      HttpMetricResults.Success
    } else if (status.isFailure() && status.intValue() < 500) {
      HttpMetricResults.UserError
    } else if (status.isFailure()) {
      HttpMetricResults.ServerError
    } else {
      warn(s"Sending unexpected response code: ${status}")
      HttpMetricResults.Unrecognised
    }

    metricsSender.incrementCount(
      metricName = s"${name}_HttpResponse_$httpMetric")
  }
}
