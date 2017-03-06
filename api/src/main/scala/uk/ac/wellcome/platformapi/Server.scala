package uk.ac.wellcome.platform.api

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter

import uk.ac.wellcome.finatra.exceptions._
import uk.ac.wellcome.finatra.modules._
import uk.ac.wellcome.platform.api.controllers._

import com.github.xiaodongw.swagger.finatra.{
  Resolvers,
  SwaggerController,
  WebjarsController
}
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import io.swagger.models.{Info, Swagger}
import io.swagger.util.Json


object ServerMain extends Server

object ApiSwagger extends Swagger {
  Json.mapper()
    .setPropertyNamingStrategy(
      new PropertyNamingStrategy.SnakeCaseStrategy)

  Resolvers.register()
}

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.api Platformapi"
  override val modules = Seq(ElasticClientModule)

  ApiSwagger.info(
    new Info()
      .description("An API")
      .version("0.0.1")
      .title("The API"))

  val swaggerController =
    new SwaggerController(swagger = ApiSwagger)

  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[WebjarsController]
      .add[ManagementController]
      .add[MainController]
      .add(swaggerController)
      .exceptionMapper[ElasticsearchExceptionMapper]
  }
}
