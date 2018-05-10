package uk.ac.wellcome.platform.sierra_item_merger

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.modules.{DynamoClientModule, MetricsSenderModule, S3ClientModule, SQSClientModule, SQSConfigModule, VHSConfigModule, _}
import modules.{AWSConfigModule, AkkaModule, SierraItemMergerModule}
import uk.ac.wellcome.sierra_adapter.modules.SierraKeyPrefixGeneratorModule
import uk.ac.wellcome.storage.vhs.VHSConfigModule

object ServerMain extends Server

class Server extends HttpServer {
  override val name =
    "uk.ac.wellcome.platform.sierra_item_merger SierraItemMerger"
  override val modules = Seq(
    DynamoClientModule,
    VHSConfigModule,
    MetricsSenderModule,
    AWSConfigModule,
    SQSConfigModule,
    SQSClientModule,
    S3ClientModule,
    AkkaModule,
    SierraItemMergerModule,
    SierraKeyPrefixGeneratorModule
  )

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
