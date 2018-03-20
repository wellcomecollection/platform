package uk.ac.wellcome.platform.api

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.finatra.json.utils.CamelCasePropertyNamingStrategy
import io.swagger.models.Swagger
import uk.ac.wellcome.elasticsearch.finatra.modules.ElasticClientModule
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.platform.api.controllers._
import uk.ac.wellcome.platform.api.finatra.exceptions.{
  CaseClassMappingExceptionWrapper,
  ElasticsearchResponseExceptionMapper,
  GeneralExceptionMapper
}
import uk.ac.wellcome.models.WorksIncludesDeserializerModule

object ServerMain extends Server
object ApiSwagger extends Swagger

object ApiJacksonModule extends FinatraJacksonModule {
  override val propertyNamingStrategy = CamelCasePropertyNamingStrategy
  override val additionalJacksonModules = Seq(
    new WorksIncludesDeserializerModule
  )
}

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.api Platformapi"
  override val modules = Seq(ElasticClientModule)

  flag(name = "api.host", default = "localhost:8888", help = "API hostname")
  flag(name = "api.scheme", default = "https", help = "API prototocol scheme")
  flag(name = "api.pageSize", default = 10, help = "API default page size")

  private final val apiName =
    flag(name = "api.name", default = "catalogue", help = "API name path part")
  private final val apiVersion =
    flag(name = "api.version", default = "v1", help = "API version path part")
  private final val apiPrefix = flag(
    name = "api.prefix",
    default = "/" + apiName() + "/" + apiVersion(),
    help = "API path prefix")

  flag[String](name = "es.index", default = "records", help = "ES index name")
  flag[String](name = "es.type", default = "item", help = "ES document type")
  flag(
    name = "api.context",
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
      .exceptionMapper[GeneralExceptionMapper]
      .exceptionMapper[CaseClassMappingExceptionWrapper]
      .exceptionMapper[ElasticsearchResponseExceptionMapper]
  }
}
