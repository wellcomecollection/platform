package uk.ac.wellcome.platform.storage.ingests.api.http

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives.mapResponse
import grizzled.slf4j.Logging
import uk.ac.wellcome.monitoring.MetricsSender

object HttpMetricResults extends Enumeration {
  type HttpMetricResults = Value
  val Success, UserError, ServerError, Unrecognised = Value
}

class HttpMetrics(name: String, metricsSender: MetricsSender) extends Logging {
  val sendCloudWatchMetrics: Directive0 = mapResponse { resp: HttpResponse =>
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

    resp
  }
}
