package uk.ac.wellcome.platform.matcher

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.controllers.ManagementController
import uk.ac.wellcome.finatra.modules.{AWSConfigModule, AkkaModule}
import uk.ac.wellcome.messaging.sns.{SNSClientModule, SNSConfigModule}
import uk.ac.wellcome.messaging.sqs.{SQSClientModule, SQSConfigModule}
import uk.ac.wellcome.monitoring.MetricsSenderModule
import uk.ac.wellcome.platform.matcher.modules.MatcherModule
import uk.ac.wellcome.storage.s3.{S3ClientModule, S3ConfigModule}

object ServerMain extends Server

class Server extends HttpServer {
  override val name =
    "uk.ac.wellcome.platform.matcher Matcher"
  override val modules = Seq(
    MetricsSenderModule,
    AWSConfigModule,
    SQSConfigModule,
    SQSClientModule,
    S3ClientModule,
    S3ConfigModule,
    SNSConfigModule,
    SNSClientModule,
    MatcherModule,
    AkkaModule
  )

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
