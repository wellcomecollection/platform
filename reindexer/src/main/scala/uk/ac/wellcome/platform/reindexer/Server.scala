package uk.ac.wellcome.platform.reindexer

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.modules.{PlatformDynamoConfigModule, DynamoClientModule, AkkaModule}
import uk.ac.wellcome.platform.reindexer.modules.ReindexModule
import uk.ac.wellcome.platform.reindexer.controllers.ManagementController


object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.reindexer Reindexer"
  override val modules = Seq(
    PlatformDynamoConfigModule,
    DynamoClientModule,
    ReindexModule,
    AkkaModule)

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
