package uk.ac.wellcome.platform.api

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.finatra.elasticsearch.{
  ElasticClientModule,
  ElasticConfigModule
}
import uk.ac.wellcome.platform.api.controllers._
import uk.ac.wellcome.platform.api.finatra.exceptions.{
  CaseClassMappingExceptionWrapper,
  ElasticsearchResponseExceptionMapper,
  GeneralExceptionMapper
}

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.api Platformapi"
  override val modules = Seq(
    ElasticClientModule,
    ElasticConfigModule
  )

  flag(name = "api.host", default = "localhost:8888", help = "API hostname")
  flag(name = "api.scheme", default = "https", help = "API protocol scheme")
  flag(name = "api.pageSize", default = 10, help = "API default page size")

  private final val apiName =
    flag(name = "api.name", default = "catalogue", help = "API name path part")
  flag[String](
    name = "api.prefix",
    default = "/" + apiName(),
    help = "API path prefix")

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
      .add[DocsController]
      .add[V1WorksController]
      .add[V2WorksController]
      .exceptionMapper[GeneralExceptionMapper]
      .exceptionMapper[CaseClassMappingExceptionWrapper]
      .exceptionMapper[ElasticsearchResponseExceptionMapper]
  }
}
