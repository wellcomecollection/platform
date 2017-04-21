package uk.ac.wellcome.platform.transformer

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.platform.transformer.controllers._
import uk.ac.wellcome.platform.transformer.modules._
import uk.ac.wellcome.transformer.modules.{
  AmazonCloudWatchModule,
  AmazonKinesisModule,
  TransformableParserModule
}

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.transformer Transformer"
  override val modules = Seq(
    KinesisWorker,
    StreamsRecordProcessorFactoryModule,
    KinesisClientLibConfigurationModule,
    AmazonKinesisModule,
    AmazonCloudWatchModule,
    DynamoConfigModule,
    AkkaModule,
    SNSConfigModule,
    SNSClientModule,
    DynamoClientModule,
    TransformableParserModule
  )

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
