package uk.ac.wellcome.platform.reindex_worker

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.controllers.ManagementController
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.platform.reindex_worker.modules.ReindexerWorkerModule

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.reindexer Reindexer"

  override val modules = Seq(
    AmazonCloudWatchModule,
    DynamoClientModule,
    DynamoConfigModule,
    SQSClientModule,
    SQSConfigModule,
    SNSClientModule,
    SNSConfigModule,
    S3ClientModule,
    S3ConfigModule,
    ReindexerWorkerModule,
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
