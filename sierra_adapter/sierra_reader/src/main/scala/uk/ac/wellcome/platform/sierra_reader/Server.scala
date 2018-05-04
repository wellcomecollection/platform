package uk.ac.wellcome.platform.sierra_reader

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
import uk.ac.wellcome.monitoring.MetricsSenderModule
import uk.ac.wellcome.messaging.sqs.{SQSClientModule, SQSConfigModule}
import uk.ac.wellcome.platform.sierra_reader.modules.SierraReaderModule
import uk.ac.wellcome.storage.s3.{S3ClientModule, S3ConfigModule}

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
