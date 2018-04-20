package uk.ac.wellcome.platform.snapshot_convertor

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.finatra.controllers.ManagementController
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.platform.snapshot_convertor.modules.{
  AkkaS3ClientModule,
  SnapshotConvertorWorkerModule
}

object ServerMain extends Server

class Server extends HttpServer {
  override val name =
    "uk.ac.wellcome.platform.snapshot_convertor SnapshotConvertor"

  override val modules = Seq(
    AmazonCloudWatchModule,
    AWSConfigModule,
    SQSClientModule,
    SQSConfigModule,
    SNSClientModule,
    SNSConfigModule,
    S3ConfigModule,
    AkkaS3ClientModule,
    S3ClientModule,
    SnapshotConvertorWorkerModule,
    AkkaModule
  )

  override def jacksonModule = DisplayJacksonModule

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
