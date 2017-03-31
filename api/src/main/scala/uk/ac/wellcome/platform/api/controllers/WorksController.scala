package uk.ac.wellcome.platform.api.controllers

import com.github.xiaodongw.swagger.finatra.SwaggerSupport

import com.twitter.inject.annotations.Flag
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext.Implicits.global

import uk.ac.wellcome.platform.api.ApiSwagger
import uk.ac.wellcome.platform.api.responses.ResultResponse
import uk.ac.wellcome.platform.api.responses.ResultListResponse
import uk.ac.wellcome.platform.api.services.CalmService
import uk.ac.wellcome.platform.api.utils.ApiRequestUtils

@Singleton
class WorksController @Inject()(
  @Flag("api.prefix") apiPrefix: String,
  @Flag("api.context") apiContext: String,
  calmService: CalmService
) extends Controller
    with SwaggerSupport {

  override implicit protected val swagger = ApiSwagger

  prefix(apiPrefix) {

    getWithDoc("/works") { doc =>
      doc
        .summary("/works")
        .description("Returns a paginated list of works")
        .tag("Works")
        .responseWith[Object](200, "ResultList[Work]")
        .queryParam[Int]("page",
                         "The page to return from the result list",
                         required = false)
        .queryParam[Int](
          "pageSize",
          "The number of works to return per page (default: 10)",
          required = false)
    } { request: Request =>
      calmService
        .findRecords()
        .map(
          results =>
            response.ok.json(
              ResultListResponse(
                context = ApiRequestUtils.hostUrl(request) + apiContext,
                results = results)))
    }

    getWithDoc("/works/:id") { doc =>
      doc
        .summary("/works/{id}")
        .description("Returns a single work")
        .tag("Works")
        .routeParam[String]("id", "The work to return", required = true)
        .responseWith[Object](200, "Work")
    } { request: Request =>
      calmService
        .findRecordByAltRefNo(request.params("id"))
        .map(
          _.map(
            result =>
              response.ok.json(ResultResponse(
                context = ApiRequestUtils.hostUrl(request) + apiContext,
                result = result)))
            .getOrElse(response.notFound))
    }

  }
}
