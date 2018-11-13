package uk.ac.wellcome.platform.transformer.miro

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
  MessageWriterConfigModule,
  SNSClientModule,
  SQSClientModule,
  SQSConfigModule
}
import uk.ac.wellcome.finatra.monitoring.MetricsSenderModule
import uk.ac.wellcome.finatra.storage.S3ClientModule
import uk.ac.wellcome.platform.transformer.miro.modules.{
  MiroTransformableModule,
  MiroTransformerWorkerModule
}

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.transformer Transformer"
  override val modules = Seq(
    MessageWriterConfigModule,
    MetricsSenderModule,
    AkkaModule,
    SQSClientModule,
    SQSConfigModule,
    SNSClientModule,
    MiroTransformerWorkerModule,
    ExecutionContextModule,
    MiroTransformableModule,
    S3ClientModule,
    TransformedBaseWorkModule
  )
  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
