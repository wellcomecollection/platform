package uk.ac.wellcome.platform.sierra_item_merger

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.akka.AkkaModule
import uk.ac.wellcome.finatra.messaging.{SQSClientModule, SQSConfigModule}
import uk.ac.wellcome.finatra.monitoring.MetricsSenderModule
import uk.ac.wellcome.finatra.controllers.ManagementController
import uk.ac.wellcome.finatra.storage.{
  DynamoClientModule,
  S3ClientModule,
  VHSConfigModule
}
import uk.ac.wellcome.platform.sierra_item_merger.modules.SierraItemMergerModule
import uk.ac.wellcome.sierra_adapter.modules.SierraTransformableModule

object ServerMain extends Server

class Server extends HttpServer {
  override val name =
    "uk.ac.wellcome.platform.sierra_item_merger SierraItemMerger"
  override val modules = Seq(
    DynamoClientModule,
    VHSConfigModule,
    MetricsSenderModule,
    SQSConfigModule,
    SQSClientModule,
    S3ClientModule,
    AkkaModule,
    SierraItemMergerModule,
    SierraTransformableModule
  )

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
