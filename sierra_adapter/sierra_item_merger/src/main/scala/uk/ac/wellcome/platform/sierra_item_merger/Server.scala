package uk.ac.wellcome.platform.sierra_item_merger

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.finatra.controllers.ManagementController
import uk.ac.wellcome.platform.sierra_adapter.modules.SierraDynamoConfigModule
import uk.ac.wellcome.platform.sierra_item_merger.modules.SierraItemMergerModule

object ServerMain extends Server

class Server extends HttpServer {
  override val name =
    "uk.ac.wellcome.platform.sierra_item_merger SierraItemMerger"
  override val modules = Seq(
    DynamoClientModule,
    AmazonCloudWatchModule,
    SQSConfigModule,
    SQSClientModule,
    AkkaModule,
    SierraDynamoConfigModule,
    SierraItemMergerModule
  )

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
