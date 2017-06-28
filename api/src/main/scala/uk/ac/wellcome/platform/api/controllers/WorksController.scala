package uk.ac.wellcome.platform.api.controllers

import javax.inject.{Inject, Singleton}

import com.github.xiaodongw.swagger.finatra.SwaggerSupport
import com.twitter.finatra.http.Controller
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.platform.api.ApiSwagger
import uk.ac.wellcome.platform.api.models.WorksIncludes
import uk.ac.wellcome.platform.api.responses.{ResultListResponse, ResultResponse}
import uk.ac.wellcome.platform.api.services.WorksService
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.platform.api.requests._


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

    val endpointSuffix = "/works"

    getWithDoc(endpointSuffix) { doc =>
      doc
        .summary(endpointSuffix)
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

      val works = request.query match {
        case Some(queryString) =>
          worksService.searchWorks(
            queryString,
            pageSize = pageSize,
            pageNumber = request.page,
            includes = request.includes.getOrElse(WorksIncludes()),
            index = request._index
          )
        case None =>
          worksService.listWorks(
            pageSize = pageSize,
            pageNumber = request.page,
            includes = request.includes.getOrElse(WorksIncludes()),
            index = request._index
          )
      }

      works
        .map(
          displaySearch => {
            ResultListResponse.create(
              contextUri,
              displaySearch,
              request,
              s"${apiScheme}://${apiHost}"
            )
          }
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
      // val includes = WorksIncludes(request.includes)
      worksService
        .findWorkById(request.id,
                      request.includes.getOrElse(WorksIncludes()),
                      index = request._index)
        .map {
          case Some(result) =>
            response.ok.json(
              ResultResponse(context = contextUri, result = result))
          case None => response.notFound
        }
    }

  }
}
