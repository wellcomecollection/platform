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
import uk.ac.wellcome.display.models.ApiVersions
import uk.ac.wellcome.display.modules.DisplayJacksonModule
import uk.ac.wellcome.finatra.akka.ExecutionContextModule
import uk.ac.wellcome.finatra.elasticsearch.{
  ElasticClientModule,
  ElasticConfigModule
}
import uk.ac.wellcome.platform.api.ContextHelper.buildContextUri
import uk.ac.wellcome.platform.api.controllers._
import uk.ac.wellcome.platform.api.finatra.exceptions.{
  CaseClassMappingExceptionWrapper,
  ElasticsearchResponseExceptionMapper,
  GeneralExceptionMapper
}
import uk.ac.wellcome.platform.api.models.{ApiConfig, DisplayError, Error}
import uk.ac.wellcome.platform.api.modules.ApiConfigModule
import uk.ac.wellcome.platform.api.responses.ResultResponse

object ServerMain extends Server

@Singleton
class AlexController @Inject()(apiConfig: ApiConfig) extends Controller {
  val contextUri: String = buildContextUri(
    apiConfig = apiConfig,
    version = ApiVersions.default
  )

  get("/:*") { request: Request =>
    val result = Error(
      variant = "http-404",
      description = Some(s"Page not found for URL ${request.uri}")
    )

    response.notFound.json(
      ResultResponse(context = contextUri, result = DisplayError(result))
    )
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
