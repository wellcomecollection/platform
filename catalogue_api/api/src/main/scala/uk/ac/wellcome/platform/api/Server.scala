package uk.ac.wellcome.platform.api

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.{Controller, HttpServer}
import com.twitter.finatra.http.filters.{
  CommonFilters,
  LoggingMDCFilter,
  TraceIdMDCFilter
}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.finatra.akka.ExecutionContextModule
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
import uk.ac.wellcome.platform.api.modules.ApiConfigModule

object ServerMain extends Server

@Singleton
class AlexController @Inject() extends Controller {
  get("/:*") { request: Request =>
    response.notFound.json(Map("message" -> "ok"))
  }
}

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.api Platformapi"
  override val modules = Seq(
    ApiConfigModule,
    ElasticClientModule,
    ElasticConfigModule,
    ExecutionContextModule
  )

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
      .add[AlexController]
      .exceptionMapper[GeneralExceptionMapper]
      .exceptionMapper[CaseClassMappingExceptionWrapper]
      .exceptionMapper[ElasticsearchResponseExceptionMapper]
  }
}
