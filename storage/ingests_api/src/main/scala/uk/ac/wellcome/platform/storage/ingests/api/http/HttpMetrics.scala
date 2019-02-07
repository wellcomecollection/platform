package uk.ac.wellcome.platform.storage.ingests.api.http

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives.mapResponse
import grizzled.slf4j.Logging
import uk.ac.wellcome.monitoring.MetricsSender

object HttpMetricResults extends Enumeration {
  type HttpMetricResults = Value
  val Success, UserError, ServerError = Value
}

class HttpMetrics(metricsSender: MetricsSender) extends Logging {
  val sendCloudWatchMetrics: Directive0 = mapResponse { resp: HttpResponse =>
    if (resp.status.isSuccess()) {
      info(s"@@AWLC Sent response SUCCESS")
    } else if (resp.status.isRedirection()) {
      info(s"@@AWLC Sent response REDIRECT")
    } else if (resp.status.isFailure()) {
      info(s"@@AWLC Sent response FAILURE")
    } else {
      warn(s"@@AWLC Sent unrecognised response code: ${resp.status}")
    }

    resp
  }
}
