package uk.ac.wellcome.platform.storage.ingests.api.http

import akka.http.scaladsl.model.HttpResponse
import akka.stream.QueueOfferResult
import grizzled.slf4j.Logging
import uk.ac.wellcome.monitoring.MetricsSender

import scala.concurrent.Future

object HttpMetricResults extends Enumeration {
  type HttpMetricResults = Value
  val Success, UserError, ServerError, Unrecognised = Value
}

class HttpMetrics(name: String, metricsSender: MetricsSender) extends Logging {

  def sendMetric(resp: HttpResponse): Future[QueueOfferResult] = {
    val httpMetric = if (resp.status.isSuccess()) {
        HttpMetricResults.Success
      } else if (resp.status.isFailure() && resp.status.intValue() < 500) {
        HttpMetricResults.UserError
      } else if (resp.status.isFailure()) {
        HttpMetricResults.ServerError
      } else {
        warn(s"Sending unexpected response code: ${resp.status}")
        HttpMetricResults.Unrecognised
      }

    metricsSender.incrementCount(metricName = s"${name}_HttpResponse_${httpMetric}")
  }
}
