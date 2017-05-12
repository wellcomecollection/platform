package uk.ac.wellcome.platform.api.controllers

import javax.inject.{Inject, Singleton}

import com.github.xiaodongw.swagger.finatra.SwaggerSupport
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.QueryParam
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.platform.api.ApiSwagger
import uk.ac.wellcome.platform.api.responses.{
  ResultListResponse,
  ResultResponse
}
import uk.ac.wellcome.platform.api.services.WorksService
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import com.twitter.finatra.validation._

case class UsersRequest(@QueryParam page: Int = 1,
                        @Min(1) @Max(100) @QueryParam pageSize: Option[Int],
                        @QueryParam query: Option[String]) {
}

@Singleton
class WorksController @Inject()(@Flag("api.prefix") apiPrefix: String,
                                @Flag("api.context") apiContext: String,
                                @Flag("api.host") apiHost: String,
                                @Flag("api.scheme") apiScheme: String,
                                @Flag("api.pageSize") defaultPageSize: Int,
                                worksService: WorksService)
    extends Controller
    with SwaggerSupport {

  override implicit protected val swagger = ApiSwagger

  val contextUri: String = s"${apiScheme}://${apiHost}${apiContext}"

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
    } { request: UsersRequest =>
      val pageSize = request.pageSize.getOrElse((defaultPageSize))

      val works = request.query match {
        case Some(queryString) =>
          worksService.searchWorks(
            queryString,
            pageSize = pageSize,
            pageNumber = request.page
          )
        case None =>
          worksService.listWorks(
            pageSize = pageSize,
            pageNumber = request.page
          )
      }

      works
        .map(
          results =>
            response.ok.json(
              ResultListResponse(
                context = contextUri,
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
              ResultResponse(context = contextUri,
                             result = result))
          case None => response.notFound
        }
    }

  }
}
