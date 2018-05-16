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
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.finatra.messaging.MessageConfigModule
import uk.ac.wellcome.monitoring.MetricsSenderModule
import uk.ac.wellcome.messaging.sqs.{SQSClientModule, SQSConfigModule}
import uk.ac.wellcome.platform.recorder.modules.{
  RecorderModule,
  RecorderWorkEntryKeyPrefixGeneratorModule
}
import uk.ac.wellcome.storage.dynamo.DynamoClientModule
import uk.ac.wellcome.storage.s3.S3ClientModule
import uk.ac.wellcome.storage.vhs.VHSConfigModule

object ServerMain extends Server

class Server extends HttpServer {
  override val name =
    "uk.ac.wellcome.platform.recorder Recorder"
  override val modules = Seq(
    VHSConfigModule,
    MessageConfigModule,
    DynamoClientModule,
    RecorderModule,
    MetricsSenderModule,
    RecorderWorkEntryKeyPrefixGeneratorModule,
    AWSConfigModule,
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
