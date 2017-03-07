package uk.ac.wellcome.platform.api

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter

import uk.ac.wellcome.finatra.exceptions._
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.platform.api.controllers._

import io.swagger.models.Swagger


object ServerMain extends Server
object ApiSwagger extends Swagger

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.api Platformapi"
  override val modules = Seq(ElasticClientModule)

  private final val apiName = flag(name = "api.name", default = "catalogue", help = "API name path part")
  private final val apiVersion = flag(name = "api.version", default = "v0", help = "API version path part")
  flag(name = "api.prefix", default = "/" + apiName() + "/" + apiVersion(), help = "API path prefix")

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[CollectionController]
      .add[ManagementController]
      .add[MainController]
      .add[SwaggerController]
      .add[SeriesController]
      .add[WorkController]
      .exceptionMapper[ElasticsearchExceptionMapper]
  }
}
