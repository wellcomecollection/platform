package uk.ac.wellcome.platform.calm_adapter

import java.util.{Timer, TimerTask}

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter

import uk.ac.wellcome.platform.calm_adapter.controllers._
import uk.ac.wellcome.platform.calm_adapter.modules._

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.calm_adapter CalmAdapter"
  override val modules =
    Seq(CalmAdapterWorker, DynamoWarmupModule, OaiHarvestConfigModule)

  private final val servicePrefix = flag(name = "service.prefix",
                                         default = "/transformer",
                                         help = "API path prefix")

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
