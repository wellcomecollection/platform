package uk.ac.wellcome.platform.snapshot_generator

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.finatra.modules.{ElasticClientModule, MetricsSenderModule, S3ConfigModule, SNSClientModule, SNSConfigModule, SQSClientModule, SQSConfigModule, _}
import modules.{AWSConfigModule, AkkaModule, AkkaS3ClientModule, SnapshotGeneratorWorkerModule}

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
    ElasticClientModule,
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
