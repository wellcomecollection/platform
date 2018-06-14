package uk.ac.wellcome.platform.merger

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.akka.{AkkaModule, ExecutionContextModule}
import uk.ac.wellcome.finatra.controllers.ManagementController
import uk.ac.wellcome.finatra.messaging.{SQSClientModule, SQSConfigModule}
import uk.ac.wellcome.finatra.monitoring.MetricsSenderModule
import uk.ac.wellcome.platform.merger.modules.MergerWorkerModule



class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.merger Merger"
  override val modules = Seq(
    MetricsSenderModule,
    AkkaModule,
    SQSClientModule,
    SQSConfigModule,
    ExecutionContextModule,
    MergerWorkerModule
  )
  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
