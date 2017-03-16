package uk.ac.wellcome.platform.calm_adapter

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter

import uk.ac.wellcome.platform.calm_adapter.controllers._
import uk.ac.wellcome.platform.calm_adapter.modules._

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.calm_adapter CalmAdapter"
  override val modules = Seq(
    CalmAdapterWorker,
    DynamoWarmupModule,
    OaiHarvestConfigModule)

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
