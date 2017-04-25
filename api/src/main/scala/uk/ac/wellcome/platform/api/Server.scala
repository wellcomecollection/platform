package uk.ac.wellcome.platform.api

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.finatra.json.utils.CamelCasePropertyNamingStrategy
import uk.ac.wellcome.finatra.exceptions._
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.platform.api.controllers._
import io.swagger.models.Swagger
import uk.ac.wellcome.platform.api.finatra.exceptions.ElasticsearchExceptionMapper

object ServerMain extends Server
object ApiSwagger extends Swagger

object ApiJacksonModule extends FinatraJacksonModule {
  override val propertyNamingStrategy = CamelCasePropertyNamingStrategy
}

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.api Platformapi"
  override val modules = Seq(ElasticClientModule)

  private final val apiName =
    flag(name = "api.name", default = "catalogue", help = "API name path part")
  private final val apiVersion =
    flag(name = "api.version", default = "v0", help = "API version path part")
  private final val apiPrefix = flag(
    name = "api.prefix",
    default = "/" + apiName() + "/" + apiVersion(),
    help = "API path prefix")

  flag(name = "api.context",
       default = apiPrefix() + "/context.json",
       help = "API JSON-LD context")

  override def jacksonModule = ApiJacksonModule

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
      .add[ContextController]
      .add[SwaggerController]
      .add[WorksController]
      .exceptionMapper[ElasticsearchExceptionMapper]
  }
}
