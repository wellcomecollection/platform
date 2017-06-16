package uk.ac.wellcome.platform.api.controllers

import javax.inject.{Inject, Singleton}

import com.github.xiaodongw.swagger.finatra.SwaggerSupport
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.{QueryParam, RouteParam}
import com.twitter.finatra.validation._
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.platform.api.ApiSwagger
import uk.ac.wellcome.platform.api.models.WorksIncludes
import uk.ac.wellcome.platform.api.responses.{ResultListResponse, ResultResponse}
import uk.ac.wellcome.platform.api.services.WorksService
import uk.ac.wellcome.utils.GlobalExecutionContext.context

case class MultipleResultsRequest(
  @Min(1) @QueryParam page: Int = 1,
  @Min(1) @Max(100) @QueryParam pageSize: Option[Int],
  @QueryParam includes: Option[String],
  @RouteParam id: Option[String],
  @QueryParam query: Option[String],
  @QueryParam _index: Option[String]
)

case class SingleWorkRequest(
  @RouteParam id: String,
  @QueryParam includes: Option[String],
  @QueryParam _index: Option[String]
)

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
        .queryParam[String](
          "includes",
          "A comma-separated list of extra fields to include",
          required = false)
        // Deliberately undocumented: we have an 'index' query param that
        // allows the user to pick which Elasticsearch index to use.  This is
        // useful for us to try out transformer changes, different index
        // weighting, etc., but we don't want to advertise its existence
        // in the public docs.
    } { request: MultipleResultsRequest =>
      val pageSize = request.pageSize.getOrElse((defaultPageSize))
      val includes = parseIncludes(request.includes)

      val works = request.query match {
        case Some(queryString) =>
          worksService.searchWorks(
            queryString,
            pageSize = pageSize,
            pageNumber = request.page,
            includes = includes,
            index = request._index
          )
        case None =>
          worksService.listWorks(
            pageSize = pageSize,
            pageNumber = request.page,
            includes = includes,
            index = request._index
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
        .queryParam[String](
          "includes",
          "A comma-separated list of extra fields to include",
          required = false)
        // Deliberately undocumented: the index flag.  See above.
    } { request: SingleWorkRequest =>
      val includes = parseIncludes(request.includes)
      worksService
        .findWorkById(request.id,
                      includes,
                      index = request._index)
        .map {
          case Some(result) =>
            response.ok.json(
              ResultResponse(context = contextUri, result = result))
          case None => response.notFound
        }
    }

  }

  /// Parse a comma-separated ?includes query parameter.
  ///
  /// TODO: Throw an exception if we get an unrecognised include.
  private def parseIncludes(includesString: Option[String]): WorksIncludes = {
    val includesList = includesString.getOrElse("").split(",").toList
    WorksIncludes(
      identifiers = includesList.contains("identifiers")
    )
  }
}
