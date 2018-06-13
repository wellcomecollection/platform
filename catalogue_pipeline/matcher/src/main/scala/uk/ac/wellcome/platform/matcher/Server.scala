package uk.ac.wellcome.platform.matcher

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.akka.{AkkaModule, ExecutionContextModule}
import uk.ac.wellcome.finatra.controllers.ManagementController
import uk.ac.wellcome.finatra.messaging.{SNSClientModule, SNSConfigModule, SQSClientModule, SQSConfigModule}
import uk.ac.wellcome.finatra.monitoring.MetricsSenderModule
import uk.ac.wellcome.finatra.storage.{DynamoClientModule, DynamoConfigModule, S3ClientModule, S3ConfigModule}
import uk.ac.wellcome.platform.matcher.modules.{DynamoLockingServiceConfigModule, MatcherModule, RecorderWorkEntryModule}

object ServerMain extends Server

class Server extends HttpServer {
  override val name =
    "uk.ac.wellcome.platform.matcher Matcher"
  override val modules = Seq(
    MetricsSenderModule,
    SQSConfigModule,
    SQSClientModule,
    S3ConfigModule,
    S3ClientModule,
    SNSConfigModule,
    SNSClientModule,
    DynamoConfigModule,
    DynamoClientModule,
    MatcherModule,
    RecorderWorkEntryModule,
    AkkaModule,
    ExecutionContextModule,
    DynamoLockingServiceConfigModule
  )

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
