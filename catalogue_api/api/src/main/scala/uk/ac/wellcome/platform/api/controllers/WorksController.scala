package uk.ac.wellcome.platform.api.controllers

import javax.inject.{Inject, Singleton}

import com.github.xiaodongw.swagger.finatra.SwaggerSupport
import com.twitter.finatra.http.Controller
import com.twitter.inject.annotations.Flag
import io.swagger.models.parameters.QueryParameter
import io.swagger.models.properties.StringProperty
import scala.collection.JavaConverters._
import uk.ac.wellcome.models.Error
import uk.ac.wellcome.platform.api.ApiSwagger
import uk.ac.wellcome.platform.api.models.{
  DisplayError,
  DisplayResultList,
  DisplayWork,
  WorksIncludes
}
import uk.ac.wellcome.platform.api.responses.{
  ResultListResponse,
  ResultResponse
}
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
  val includesSwaggerParam: QueryParameter = new QueryParameter()
    .name("includes")
    .description("A comma-separated list of extra fields to include")
    .required(false)
    .`type`("array")
    .collectionFormat("csv")
    .items(new StringProperty()._enum(WorksIncludes.recognisedIncludes.asJava))

  prefix(apiPrefix) {

    val endpointSuffix = "/works"

    getWithDoc(endpointSuffix) { doc =>
      doc
        .summary(endpointSuffix)
        .description("Returns a paginated list of works")
        .tag("Works")
        .responseWith[DisplayResultList](200, "ResultList[Work]")
        .responseWith[DisplayError](400, "Bad Request Error")
        .responseWith[DisplayError](404, "Not Found Error")
        .responseWith[DisplayError](500, "Internal Server Error")
        .queryParam[Int](
          "page",
          "The page to return from the result list",
          required = false)
        .queryParam[Int](
          "pageSize",
          "The number of works to return per page (default: 10)",
          required = false)
        .queryParam[String](
          "query",
          """Full-text search query, which will OR supplied terms by default.
            |
            |The following special characters can be used to change the search behaviour:
            |
            |- \+ signifies AND operation
            |- | signifies OR operation
            |- \- negates a single token
            |- " wraps a number of tokens to signify a phrase for searching
            |- \* at the end of a term signifies a prefix query
            |- ( and ) signify precedence
            |- ~N after a word signifies edit distance (fuzziness)
            |- ~N after a phrase signifies slop amount
            |
            |To search for any of these special characters, they should be escaped with \.""".stripMargin,
          required = false
        )
        .parameter(includesSwaggerParam)
    // Deliberately undocumented: we have an 'index' query param that
    // allows the user to pick which Elasticsearch index to use.  This is
    // useful for us to try out transformer changes, different index
    // weighting, etc., but we don't want to advertise its existence
    // in the public docs.

    } { request: MultipleResultsRequest =>
      val pageSize = request.pageSize.getOrElse((defaultPageSize))
      val includes = request.includes.getOrElse(WorksIncludes())

      val works = request.query match {
        case Some(queryString) =>
          worksService.searchWorks(
            queryString,
            pageSize = pageSize,
            pageNumber = request.page,
            index = request._index
          )
        case None =>
          worksService.listWorks(
            pageSize = pageSize,
            pageNumber = request.page,
            index = request._index
          )
      }

      works
        .map { resultList =>
          DisplayResultList(
            results = resultList.results.map { DisplayWork(_, includes) }.toArray,
            pageSize = pageSize,
            totalPages = Math.ceil(resultList.totalResults.toDouble / pageSize.toDouble).toInt,
            totalResults = resultList.totalResults
          )
        }
        .map(
          displayResultList => {
            ResultListResponse.create(
              contextUri,
              displayResultList,
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
        .responseWith[DisplayWork](200, "Work")
        .responseWith[DisplayError](400, "Bad Request Error")
        .responseWith[DisplayError](404, "Not Found Error")
        .responseWith[DisplayError](410, "Gone Error")
        .responseWith[DisplayError](500, "Internal Server Error")
        .parameter(includesSwaggerParam)
    // Deliberately undocumented: the index flag.  See above.
    } { request: SingleWorkRequest =>
      val includes = request.includes.getOrElse(WorksIncludes())
      worksService
        .findWorkById(canonicalId = request.id, index = request._index)
        .map {
          case Some(work) => DisplayWork(work = work, includes = includes)
          case None => None
        }
        .map {

          // If the work is visible, we return the complete work.  If it's
          // present but hidden, we need to return an HTTP 410 Gone.
          case Some(work: DisplayWork) =>
            if (work.visible) {
              response.ok.json(
                ResultResponse(context = contextUri, result = work))
            } else {
              val result = Error(
                variant = "http-410",
                description = Some("This work has been deleted")
              )
              response.gone.json(
                ResultResponse(
                  context = contextUri,
                  result = DisplayError(result)
                )
              )
            }

          case None => {
            val result = Error(
              variant = "http-404",
              description =
                Some(s"Work not found for identifier ${request.id}")
            )
            response.notFound.json(
              ResultResponse(
                context = contextUri,
                result = DisplayError(result))
            )
          }
        }
    }

  }
}
