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
import uk.ac.wellcome.messaging.message.MessageConfigModule
import uk.ac.wellcome.messaging.sns.{SNSClientModule, SNSConfigModule}
import uk.ac.wellcome.messaging.sqs.{SQSClientModule, SQSConfigModule}
import uk.ac.wellcome.storage.dynamo.{DynamoClientModule, DynamoConfigModule}
import uk.ac.wellcome.storage.s3.{S3ClientModule, S3ConfigModule}
import uk.ac.wellcome.transformer.modules.TransformerWorkerModule
import uk.ac.wellcome.transformer.modules._

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.transformer Transformer"
  override val modules = Seq(
    AmazonCloudWatchModule,
    AWSConfigModule,
    DynamoConfigModule,
    AkkaModule,
    SQSClientModule,
    SQSConfigModule,
    SNSConfigModule,
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
