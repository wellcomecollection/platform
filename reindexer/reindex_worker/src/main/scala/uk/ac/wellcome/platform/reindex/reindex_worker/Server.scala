package uk.ac.wellcome.platform.reindex.reindex_worker

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.akka.{AkkaModule, ExecutionContextModule}
import uk.ac.wellcome.finatra.controllers.ManagementController
import uk.ac.wellcome.finatra.messaging.{
  SNSClientModule,
  SNSConfigModule,
  SQSClientModule,
  SQSConfigModule
}
import uk.ac.wellcome.finatra.monitoring.MetricsSenderModule
import uk.ac.wellcome.finatra.storage.{
  DynamoClientModule,
  DynamoConfigModule,
  S3ClientModule
}
import uk.ac.wellcome.platform.reindex.reindex_worker.modules.ReindexerWorkerModule

object ServerMain extends Server

class Server extends HttpServer {
  override val name =
    "uk.ac.wellcome.platform.reindex.creator ReindexRequestCreator"

  override val modules = Seq(
    ExecutionContextModule,
    MetricsSenderModule,
    DynamoClientModule,
    DynamoConfigModule,
    SNSClientModule,
    SNSConfigModule,
    SQSClientModule,
    SQSConfigModule,
    S3ClientModule,
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
