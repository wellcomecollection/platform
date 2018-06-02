package uk.ac.wellcome.platform.ingestor

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
import uk.ac.wellcome.finatra.elasticsearch.{
  ElasticClientModule,
  ElasticConfigModule
}
import uk.ac.wellcome.finatra.messaging.{MessageConfigModule, SQSClientModule}
import uk.ac.wellcome.finatra.monitoring.MetricsSenderModule
import uk.ac.wellcome.finatra.storage.{S3ClientModule, S3ConfigModule}
import uk.ac.wellcome.platform.ingestor.modules.{
  ExecutionContextModule,
  IdentifiedWorkModule,
  IngestorWorkerModule,
  WorksIndexModule
}

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.ingestor Ingestor"
  override val modules = Seq(
    MetricsSenderModule,
    SQSClientModule,
    MessageConfigModule,
    S3ConfigModule,
    S3ClientModule,
    AkkaModule,
    IngestorWorkerModule,
    ElasticClientModule,
    ElasticConfigModule,
    ExecutionContextModule,
    WorksIndexModule,
    IdentifiedWorkModule
  )

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
