package uk.ac.wellcome.finatra.filters

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.Service
import com.twitter.finagle.filter.LogFormatter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.filters.AccessLoggingFilter
import com.twitter.util.Future

/** Suppress logs for healthcheck requests from the ALB.
  *
  * Specifically, it suppresses log messages such as the following:
  *
  *     10:14:09.578 [finagle/netty4-7] INFO c.t.f.h.filters.AccessLoggingFilter -
  *         172.17.0.5 - - [14/Jun/2018:10:14:09 +0000]
  *         "GET /management/healthcheck HTTP/1.0" 200 16 0 "ELB-HealthChecker/2.0"
  *
  * This helps keep our logs clean -- we know that healthchecks are working
  * unless an application gets stopped by ECS, and then we can re-enable these
  * logs by setting the log level to DEBUG.
  */
@Singleton
class PlatformLoggingFilter @Inject()(
  logFormatter: LogFormatter[Request, Response]
) extends AccessLoggingFilter[Request](logFormatter = logFormatter) {

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {
    if (
      request.userAgent.contains("ELB-HealthChecker/2.0") &&
      request.path == "/management/healthcheck" &&
      !isDebugEnabled
    ) {
      service(request)
    } else {
      super.apply(request = request, service = service)
    }
  }
}
