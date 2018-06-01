package uk.ac.wellcome.platform.recorder

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
import uk.ac.wellcome.finatra.messaging.{
  MessageConfigModule,
  SQSClientModule,
  SQSConfigModule
}
import uk.ac.wellcome.finatra.monitoring.MetricsSenderModule
import uk.ac.wellcome.finatra.storage.{
  DynamoClientModule,
  S3ClientModule,
  VHSConfigModule
}
import uk.ac.wellcome.platform.recorder.modules.{
  ExecutionContextModule,
  RecorderModule,
  RecorderWorkEntryModule,
  UnidentifiedWorkModule
}

object ServerMain extends Server

class Server extends HttpServer {
  override val name =
    "uk.ac.wellcome.platform.recorder Recorder"
  override val modules = Seq(
    ExecutionContextModule,
    VHSConfigModule,
    MessageConfigModule,
    DynamoClientModule,
    RecorderModule,
    MetricsSenderModule,
    RecorderWorkEntryModule,
    UnidentifiedWorkModule,
    SQSConfigModule,
    SQSClientModule,
    S3ClientModule,
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
