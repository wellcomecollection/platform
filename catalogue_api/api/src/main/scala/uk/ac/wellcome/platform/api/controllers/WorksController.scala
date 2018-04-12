package uk.ac.wellcome.platform.api.controllers

import com.github.xiaodongw.swagger.finatra.SwaggerSupport
import com.twitter.finatra.http.Controller
import com.twitter.inject.annotations.Flag
import io.swagger.models.Operation
import io.swagger.models.parameters.QueryParameter
import io.swagger.models.properties.StringProperty
import javax.inject.{Inject, Singleton}
import uk.ac.wellcome.display.models.WorksIncludes
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.models.{Error, IdentifiedWork}
import uk.ac.wellcome.platform.api.ApiSwagger
import uk.ac.wellcome.platform.api.ContextHelper.buildContextUri
import uk.ac.wellcome.platform.api.models.DisplayError
import uk.ac.wellcome.platform.api.models.v1.DisplayResultListV1
import uk.ac.wellcome.platform.api.requests._
import uk.ac.wellcome.platform.api.responses.{ResultListResponse, ResultResponse}
import uk.ac.wellcome.platform.api.services.WorksService
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.collection.JavaConverters._

@Singleton
class WorksController @Inject()(
  @Flag("api.prefix") apiPrefix: String,
  @Flag("api.context.suffix") apiContextSuffix: String,
  @Flag("api.host") apiHost: String,
  @Flag("api.scheme") apiScheme: String,
  @Flag("api.pageSize") defaultPageSize: Int,
  worksService: WorksService)
    extends Controller
    with SwaggerSupport {

  override implicit protected val swagger = ApiSwagger

  val includesSwaggerParam: QueryParameter = new QueryParameter()
    .name("includes")
    .description("A comma-separated list of extra fields to include")
    .required(false)
    .`type`("array")
    .collectionFormat("csv")
    .items(new StringProperty()._enum(WorksIncludes.recognisedIncludes.asJava))

  prefix(apiPrefix) {
    setupResultListEndpoint("/v1", "/works")
    setupSingleWorkEndpoint("/v1", "/works/:id")
  }

  private def setupResultListEndpoint(version: String,
                                      endpointSuffix: String): Unit = {
    getWithDoc(s"$version$endpointSuffix") { doc =>
      setupResultListSwaggerDocs(s"$version$endpointSuffix", doc)
    } { request: MultipleResultsRequest =>
      val pageSize = request.pageSize.getOrElse(defaultPageSize)
      val includes = request.includes.getOrElse(WorksIncludes())

      for {
        resultList <- getWorkList(request, pageSize)
        displayResultList = DisplayResultListV1(
          resultList = resultList,
          pageSize = pageSize,
          includes = includes
        )
      } yield
        ResultListResponse.create(
          buildContextUri(
            apiScheme = apiScheme,
            apiHost = apiHost,
            apiPrefix = apiPrefix,
            version = version,
            apiContextSuffix = apiContextSuffix),
          displayResultList,
          request,
          s"$apiScheme://$apiHost"
        )
    }
  }

  private def setupSingleWorkEndpoint(version: String,
                                      endpointSuffix: String): Unit = {
    getWithDoc(s"$version$endpointSuffix") { doc =>
      setUpSingleWorkSwaggerDocs(version, doc)
    } { request: SingleWorkRequest =>
      val includes = request.includes.getOrElse(WorksIncludes())

      val contextUri = buildContextUri(
        apiScheme = apiScheme,
        apiHost = apiHost,
        apiPrefix = apiPrefix,
        version = version,
        apiContextSuffix = apiContextSuffix)
      val eventualResponse = for {
        maybeWork <- worksService.findWorkById(
          canonicalId = request.id,
          index = request._index)
      } yield
        generateSingleWorkResponse(maybeWork, includes, request, contextUri)

      eventualResponse.recover {
        // If a user tries to request an ID without escaping it correctly
        // (e.g. "/works/work/zd224ncv]"), we get an IllegalArgumentException with
        // the error:
        //
        //      Illegal character in path at index 20: /works/work/zd224ncv]
        //
        // In this case, we return a 400 Bad Request exception rather than bubbling
        // up as a 500 error.
        case exception: IllegalArgumentException =>
          if (exception.getMessage.startsWith(
                "Illegal character in path at index ")) {
            val result = Error(
              variant = "http-400",
              description =
                Some(s"Unrecognised character in identifier ${request.id}")
            )
            response.badRequest.json(
              ResultResponse(
                context = contextUri,
                result = DisplayError(result)
              )
            )
          }
      }
    }
  }

  private def getWorkList(request: MultipleResultsRequest, pageSize: Int) = {
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
  }

  private def generateSingleWorkResponse(maybeWork: Option[IdentifiedWork],
                                         includes: WorksIncludes,
                                         request: SingleWorkRequest,
                                         contextUri: String) =
    maybeWork match {
      case Some(work: IdentifiedWork) =>
        if (work.visible) {
          respondWithWork(includes, work, contextUri: String)
        } else {
          respondWithGoneError(contextUri: String)
        }
      case None =>
        respondWithNotFoundError(request, contextUri: String)
    }

  private def respondWithWork(includes: WorksIncludes, work: IdentifiedWork, contextUri: String) = {
    val result = DisplayWorkV1(work = work, includes = includes)
    response.ok.json(ResultResponse(context = contextUri, result = result))
  }

  private def respondWithGoneError(contextUri: String) = {
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

  private def respondWithNotFoundError(request: SingleWorkRequest,
                                       contextUri: String) = {
    val result = Error(
      variant = "http-404",
      description = Some(s"Work not found for identifier ${request.id}")
    )
    response.notFound.json(
      ResultResponse(context = contextUri, result = DisplayError(result))
    )
  }

  private def setupResultListSwaggerDocs(endpointSuffix: String,
                                         doc: Operation) = {
    doc
      .summary(endpointSuffix)
      .description("Returns a paginated list of works")
      .tag("Works")
      .responseWith[DisplayResultListV1](200, "ResultList[Work]")
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
  }

  private def setUpSingleWorkSwaggerDocs(version: String, doc: Operation) = {
    doc
      .summary(s"$version/works/{id}")
      .description("Returns a single work")
      .tag("Works")
      .routeParam[String]("id", "The work to return", required = true)
      .responseWith[DisplayWorkV1](200, "Work")
      .responseWith[DisplayError](400, "Bad Request Error")
      .responseWith[DisplayError](404, "Not Found Error")
      .responseWith[DisplayError](410, "Gone Error")
      .responseWith[DisplayError](500, "Internal Server Error")
      .parameter(includesSwaggerParam)
    // Deliberately undocumented: the index flag.  See above.
  }
}
