package uk.ac.wellcome.transformer

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
import uk.ac.wellcome.transformer.modules.{
  TransformerModule,
  TransformerWorkerModule
}

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.transformer Transformer"
  override val modules = Seq(
    AmazonCloudWatchModule,
    AWSConfigModule,
    PlatformDynamoConfigModule,
    AkkaModule,
    SQSClientModule,
    SQSConfigModule,
    SNSConfigModule,
    SNSClientModule,
    TransformerWorkerModule,
    TransformerModule,
    S3ClientModule,
    S3ConfigModule
  )
  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
