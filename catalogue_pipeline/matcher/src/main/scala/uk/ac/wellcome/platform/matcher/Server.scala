package uk.ac.wellcome.platform.matcher

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.akka.{AkkaModule, ExecutionContextModule}
import uk.ac.wellcome.finatra.controllers.ManagementController
import uk.ac.wellcome.finatra.messaging._
import uk.ac.wellcome.finatra.monitoring.MetricsSenderModule
import uk.ac.wellcome.finatra.storage.{DynamoClientModule, DynamoConfigModule, S3ClientModule}
import uk.ac.wellcome.platform.matcher.modules.{DynamoLockingServiceConfigModule, MatcherModule, TransformedBaseWorkModule}

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.matcher Matcher"
  override val modules = Seq(
    SQSClientModule,
    S3ClientModule,
    SNSConfigModule,
    SNSClientModule,
    MessageReaderConfigModule,
    MetricsSenderModule,
    DynamoConfigModule,
    DynamoClientModule,
    MatcherModule,
    TransformedBaseWorkModule,
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
