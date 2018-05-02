package uk.ac.wellcome.platform.idminter

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.controllers.ManagementController
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.messaging.message.MessageConfigModule
import uk.ac.wellcome.messaging.sns.{SNSClientModule, SNSConfigModule}
import uk.ac.wellcome.messaging.sqs.{SQSClientModule, SQSConfigModule, SQSReaderModule}
import uk.ac.wellcome.platform.idminter.modules._
import uk.ac.wellcome.storage.s3.S3ClientModule

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.id_minter IdMinter"
  override val modules = Seq(
    MysqlModule,
    AkkaModule,
    IdMinterWorkerModule,
    AWSConfigModule,
    SQSClientModule,
    SQSConfigModule,
    SQSReaderModule,
    SNSConfigModule,
    SNSClientModule,
    S3ClientModule,
    MessageConfigModule,
    JsonKeyPrefixGeneratorModule,
    AmazonCloudWatchModule
  )

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
