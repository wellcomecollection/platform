package uk.ac.wellcome.platform.reindex_worker

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
import uk.ac.wellcome.messaging.metrics.MetricsSenderModule
import uk.ac.wellcome.messaging.sns.{SNSClientModule, SNSConfigModule}
import uk.ac.wellcome.messaging.sqs.{SQSClientModule, SQSConfigModule}
import uk.ac.wellcome.platform.reindex_worker.modules.ReindexerWorkerModule
import uk.ac.wellcome.storage.dynamo.{DynamoClientModule, DynamoConfigModule}
import uk.ac.wellcome.storage.s3.{S3ClientModule, S3ConfigModule}

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.reindexer Reindexer"

  override val modules = Seq(
    MetricsSenderModule,
    DynamoClientModule,
    DynamoConfigModule,
    SQSClientModule,
    SQSConfigModule,
    SNSClientModule,
    SNSConfigModule,
    S3ClientModule,
    S3ConfigModule,
    ReindexerWorkerModule,
    AkkaModule
  )

  flag[String](
    "aws.dynamo.indexName",
    "reindexTracker",
    "Name of the reindex tracker GSI")

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
