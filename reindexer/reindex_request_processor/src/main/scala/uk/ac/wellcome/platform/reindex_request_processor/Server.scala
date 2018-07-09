package uk.ac.wellcome.platform.reindex_request_processor

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.akka.AkkaModule
import uk.ac.wellcome.finatra.controllers.ManagementController
import uk.ac.wellcome.finatra.messaging.{SQSClientModule, SQSConfigModule}
import uk.ac.wellcome.platform.reindex_request_processor.modules.ReindexerWorkerModule

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.reindex_request_processor ReindexRequestCreator"

  override val modules = Seq(
    AkkaModule,
    SQSClientModule,
    SQSConfigModule,
    ReindexerWorkerModule
  )

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
