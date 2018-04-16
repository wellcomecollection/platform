package uk.ac.wellcome.platform.api

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter
import io.swagger.models.Swagger
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.elasticsearch.finatra.modules.ElasticClientModule
import uk.ac.wellcome.platform.api.controllers._
import uk.ac.wellcome.platform.api.finatra.exceptions.{
  CaseClassMappingExceptionWrapper,
  ElasticsearchResponseExceptionMapper,
  GeneralExceptionMapper
}

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.api Platformapi"
  override val modules = Seq(ElasticClientModule)

  flag(name = "api.host", default = "localhost:8888", help = "API hostname")
  flag(name = "api.scheme", default = "https", help = "API prototocol scheme")
  flag(name = "api.pageSize", default = 10, help = "API default page size")

  private final val apiName =
    flag(name = "api.name", default = "catalogue", help = "API name path part")
  private final val apiPrefix = flag(
    name = "api.prefix",
    default = "/" + apiName(),
    help = "API path prefix")

  flag[String](name = "es.index.v1", help = "V1 ES index name")
  flag[String](name = "es.index.v2", help = "V2 ES index name")

  flag[String](name = "es.type", default = "item", help = "ES document type")
  flag(
    name = "api.context.suffix",
    default = "/context.json",
    help = "Relative API JSON-LD context")

  override def jacksonModule = DisplayJacksonModule

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
      .add[ContextController]
      .add[SwaggerController]
      .add[V1WorksController]
      .add[V2WorksController]
      .exceptionMapper[GeneralExceptionMapper]
      .exceptionMapper[CaseClassMappingExceptionWrapper]
      .exceptionMapper[ElasticsearchResponseExceptionMapper]
  }
}
