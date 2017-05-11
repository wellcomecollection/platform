package uk.ac.wellcome.platform.api.controllers

import javax.inject.{Inject, Singleton}

import com.github.xiaodongw.swagger.finatra.SwaggerSupport
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.platform.api.ApiSwagger
import uk.ac.wellcome.platform.api.responses.{ResultListResponse, ResultResponse}
import uk.ac.wellcome.platform.api.services.WorksService
import uk.ac.wellcome.platform.api.utils.ApiRequestUtils
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.Future

@Singleton
class WorksController @Inject()(@Flag("api.prefix") apiPrefix: String,
                                @Flag("api.context") apiContext: String,
                                @Flag("api.host") apiHost: String,
                                worksService: WorksService)
    extends Controller
    with SwaggerSupport
    with ApiRequestUtils {

  override implicit protected val swagger = ApiSwagger

  override val hostName: String = apiHost

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
        .queryParam[String]("query",
                            "Full-text search query",
                            required = false)
    } { request: Request =>
      val works = request.params.get("query") match {
        case Some(queryString) => worksService.searchWorks(queryString)
        case None => worksService.findWorks()
      }

      works
        .map(
          results =>
            response.ok.json(
              ResultListResponse(
                context = hostUrl(request) + apiContext,
                results = results.results,
                pageSize = results.pageSize,
                totalPages = results.totalPages,
                totalResults = results.totalResults
              )
            )
          )
    }

    getWithDoc("/works/:id") { doc =>
      doc
        .summary("/works/{id}")
        .description("Returns a single work")
        .tag("Works")
        .routeParam[String]("id", "The work to return", required = true)
        .responseWith[Object](200, "Work")
    } { request: Request =>
      worksService
        .findWorkById(request.params("id"))
        .map {
          case Some(result) =>
            response.ok.json(
              ResultResponse(context = hostUrl(request) + apiContext,
                             result = result))
          case None => response.notFound
        }
    }

  }
}
