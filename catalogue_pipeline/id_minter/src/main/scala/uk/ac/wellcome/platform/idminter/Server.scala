package uk.ac.wellcome.platform.idminter

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
import uk.ac.wellcome.finatra.messaging.{
  MessageReaderConfigModule,
  MessageWriterConfigModule,
  SNSClientModule,
  SQSClientModule
}
import uk.ac.wellcome.finatra.monitoring.MetricsSenderModule
import uk.ac.wellcome.finatra.storage.S3ClientModule
import uk.ac.wellcome.platform.idminter.modules._

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.id_minter IdMinter"
  override val modules = Seq(
    MysqlModule,
    IdentifiersTableConfigModule,
    AkkaModule,
    ExecutionContextModule,
    IdMinterWorkerModule,
    MessageReaderConfigModule,
    MessageWriterConfigModule,
    SQSClientModule,
    SNSClientModule,
    S3ClientModule,
    JsonModule,
    MetricsSenderModule
  )

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
