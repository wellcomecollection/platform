package uk.ac.wellcome.platform.snapshot_generator

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
import uk.ac.wellcome.monitoring.MetricsSenderModule
import uk.ac.wellcome.finatra.messaging.{SNSClientModule, SNSConfigModule}
import uk.ac.wellcome.messaging.sqs.{SQSClientModule, SQSConfigModule}
import uk.ac.wellcome.platform.snapshot_generator.finatra.SnapshotGeneratorModule
import uk.ac.wellcome.platform.snapshot_generator.finatra.modules.{
  AkkaS3ClientModule,
  SnapshotGeneratorWorkerModule
}
import uk.ac.wellcome.storage.s3.S3ConfigModule

object ServerMain extends Server

class Server extends HttpServer {

  flag[String](name = "es.index.v1", help = "V1 ES index name")
  flag[String](name = "es.index.v2", help = "V2 ES index name")

  flag[String](name = "es.type", default = "item", help = "ES document type")

  override val name =
    "uk.ac.wellcome.platform.snapshot_generator SnapshotGenerator"

  override val modules = Seq(
    MetricsSenderModule,
    AWSConfigModule,
    SQSClientModule,
    SQSConfigModule,
    SNSClientModule,
    SNSConfigModule,
    S3ConfigModule,
    AkkaS3ClientModule,
    SnapshotGeneratorModule,
    SnapshotGeneratorWorkerModule,
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
