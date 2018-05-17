package uk.ac.wellcome.platform.recorder

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.controllers.ManagementController
import uk.ac.wellcome.finatra.messaging.{
  MessageConfigModule,
  SQSClientModule,
  SQSConfigModule
}
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.finatra.storage.VHSClientModule
import uk.ac.wellcome.monitoring.MetricsSenderModule
import uk.ac.wellcome.platform.recorder.modules.{
  RecorderModule,
  RecorderWorkEntryKeyPrefixGeneratorModule
}

object ServerMain extends Server

class Server extends HttpServer {
  override val name =
    "uk.ac.wellcome.platform.recorder Recorder"
  override val modules = Seq(
    VHSClientModule,
    MessageConfigModule,
    RecorderModule,
    MetricsSenderModule,
    RecorderWorkEntryKeyPrefixGeneratorModule,
    AWSConfigModule,
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
