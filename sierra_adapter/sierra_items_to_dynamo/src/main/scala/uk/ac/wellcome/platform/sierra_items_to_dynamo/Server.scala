package uk.ac.wellcome.platform.sierra_items_to_dynamo

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.modules.{DynamoClientModule, DynamoConfigModule, MetricsSenderModule, SQSClientModule, SQSConfigModule, _}
import modules.{AkkaModule, SierraItemsToDynamoModule}
import uk.ac.wellcome.storage.dynamo.DynamoConfigModule

object ServerMain extends Server

class Server extends HttpServer {
  override val name =
    "uk.ac.wellcome.platform.sierra_items_to_dynamo SierraItemsToDynamo"
  override val modules = Seq(
    SierraItemsToDynamoModule,
    DynamoConfigModule,
    DynamoClientModule,
    MetricsSenderModule,
    SQSConfigModule,
    SQSClientModule,
    AkkaModule
  )

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
