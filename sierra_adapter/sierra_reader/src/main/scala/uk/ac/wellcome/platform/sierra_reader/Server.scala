package uk.ac.wellcome.platform.sierra_reader

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.modules.{MetricsSenderModule, S3ClientModule, S3ConfigModule, SQSClientModule, SQSConfigModule, _}
import modules.{AWSConfigModule, AkkaModule, SierraReaderModule}

object ServerMain extends Server

class Server extends HttpServer {
  override val name =
    "uk.ac.wellcome.platform.sierra_reader SierraReader"
  override val modules = Seq(
    SierraReaderModule,
    MetricsSenderModule,
    AWSConfigModule,
    S3ClientModule,
    S3ConfigModule,
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
