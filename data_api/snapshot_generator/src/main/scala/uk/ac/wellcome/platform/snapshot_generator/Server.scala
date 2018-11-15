package uk.ac.wellcome.platform.snapshot_generator

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
import uk.ac.wellcome.finatra.elasticsearch.{
  ElasticClientModule,
  ElasticConfigModule
}
import uk.ac.wellcome.finatra.messaging.{
  SNSClientModule,
  SNSConfigModule,
  SQSClientModule,
  SQSConfigModule
}
import uk.ac.wellcome.finatra.monitoring.MetricsSenderModule
import uk.ac.wellcome.platform.snapshot_generator.finatra.modules.{
  AkkaS3ClientModule,
  SnapshotGeneratorWorkerModule
}

object ServerMain extends Server

class Server extends HttpServer {

  override val name =
    "uk.ac.wellcome.platform.snapshot_generator SnapshotGenerator"

  override val modules = Seq(
    MetricsSenderModule,
    SQSClientModule,
    SQSConfigModule,
    SNSClientModule,
    SNSConfigModule,
    AkkaS3ClientModule,
    ElasticClientModule,
    ElasticConfigModule,
    SnapshotGeneratorWorkerModule,
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
