package uk.ac.wellcome.platform.api

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter

import uk.ac.wellcome.platform.api.controllers._
import uk.ac.wellcome.platform.api.modules._
import uk.ac.wellcome.platform.api.exceptions._

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.api Platformapi"
  override val modules = Seq(
    ElasticClientModule)

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
      .add[MainController]
      .exceptionMapper[ElasticsearchExceptionMapper]
  }
}
