package uk.ac.wellcome.platform.sierra_reader

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.akka.{AkkaModule, ExecutionContextModule}
import uk.ac.wellcome.finatra.messaging.{SQSClientModule, SQSConfigModule}
import uk.ac.wellcome.finatra.controllers.ManagementController
import uk.ac.wellcome.finatra.modules.AccessLoggingFilterModule
import uk.ac.wellcome.finatra.monitoring.MetricsSenderModule
import uk.ac.wellcome.finatra.storage.{S3ClientModule, S3ConfigModule}
import uk.ac.wellcome.platform.sierra_reader.modules.{
  ReaderConfigModule,
  SierraConfigModule,
  SierraReaderModule
}

object ServerMain extends Server

class Server extends HttpServer {
  override val name =
    "uk.ac.wellcome.platform.sierra_reader SierraReader"
  override val modules = Seq(
    AccessLoggingFilterModule,
    SierraReaderModule,
    ReaderConfigModule,
    SierraConfigModule,
    MetricsSenderModule,
    S3ClientModule,
    S3ConfigModule,
    SQSConfigModule,
    SQSClientModule,
    AkkaModule,
    ExecutionContextModule
  )

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
