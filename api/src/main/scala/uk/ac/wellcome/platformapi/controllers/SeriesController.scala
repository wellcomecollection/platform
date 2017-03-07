package uk.ac.wellcome.platform.api.controllers

import com.github.xiaodongw.swagger.finatra.SwaggerSupport

import com.twitter.inject.annotations.Flag
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

import javax.inject.{Inject, Singleton}

import uk.ac.wellcome.platform.api.ApiSwagger


@Singleton
class SeriesController @Inject()(
  @Flag("api.prefix") apiPrefix: String) extends Controller with SwaggerSupport {

  override implicit protected val swagger = ApiSwagger

  prefix(apiPrefix) {

    getWithDoc("/series") { doc =>
      doc.summary("/series")
        .description("Returns a paginated list of series")
        .tag("Series")
        .queryParam[Int]("page", "The page to return from the result list", required = false)
        .queryParam[Int]("pageSize", "The number of series to return per page (default: 10)", required = false)
        .responseWith[Object](200, "ResultList[Series]")
    } { request: Request =>
      response.notImplemented
    }

    getWithDoc("/series/:id") { doc =>
      doc.summary("/series/{id}")
        .description("Returns a single series")
        .tag("Series")
        .routeParam[String]("id", "The series to return",  required = true)
        .responseWith[Object](200, "Series")
    } { request: Request =>
      response.notImplemented
    }

  }
}
