package uk.ac.wellcome.transformer

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.modules.{DynamoConfigModule, MessageConfigModule, MetricsSenderModule, S3ClientModule, S3ConfigModule, SNSClientModule, SNSConfigModule, SQSClientModule, SQSConfigModule, _}
import uk.ac.wellcome.storage.dynamo.DynamoConfigModule
import modules.{AWSConfigModule, AkkaModule, TransformerWorkerModule, _}

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.transformer Transformer"
  override val modules = Seq(
    MetricsSenderModule,
    AWSConfigModule,
    DynamoConfigModule,
    AkkaModule,
    SQSClientModule,
    SQSConfigModule,
    SNSClientModule,
    TransformerWorkerModule,
    S3ClientModule,
    S3ConfigModule,
    MessageConfigModule,
    UnidentifiedWorkKeyPrefixGeneratorModule
  )
  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
