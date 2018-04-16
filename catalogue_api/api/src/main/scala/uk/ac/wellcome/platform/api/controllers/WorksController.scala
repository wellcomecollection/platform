package uk.ac.wellcome.platform.api.controllers

import com.github.xiaodongw.swagger.finatra.{FinatraOperation, FinatraSwagger, SwaggerSupport}
import com.twitter.finagle.http.RouteIndex

import reflect.runtime.universe.TypeTag
import com.twitter.finatra.http.{Controller, RouteDSL, SwaggerRouteDSL}
import com.twitter.inject.annotations.Flag
import io.swagger.models.{Operation, Swagger}
import io.swagger.models.parameters.QueryParameter
import io.swagger.models.properties.StringProperty
import javax.inject.{Inject, Singleton}
import uk.ac.wellcome.display.models.{DisplayWork, WorksIncludes}
import uk.ac.wellcome.display.models.v1.DisplayWorkV1
import uk.ac.wellcome.display.models.v2.DisplayWorkV2
import uk.ac.wellcome.models.{ApiVersions, Error, IdentifiedWork}
import uk.ac.wellcome.platform.api.ContextHelper.buildContextUri
import uk.ac.wellcome.platform.api.models.{DisplayError, DisplayResultList}
import uk.ac.wellcome.platform.api.requests._
import uk.ac.wellcome.platform.api.responses.{ResultListResponse, ResultResponse}
import uk.ac.wellcome.platform.api.services.WorksService
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import scala.language.implicitConversions

import scala.collection.JavaConverters._

@Singleton
class WorksController @Inject()(
  @Flag("api.prefix") apiPrefix: String,
  @Flag("api.context.suffix") apiContextSuffix: String,
  @Flag("api.host") apiHost: String,
  @Flag("api.scheme") apiScheme: String,
  @Flag("api.pageSize") defaultPageSize: Int,
  worksService: WorksService)
    extends Controller {

  protected val dsl = this
  val includesSwaggerParam: QueryParameter = new QueryParameter()
    .name("includes")
    .description("A comma-separated list of extra fields to include")
    .required(false)
    .`type`("array")
    .collectionFormat("csv")
    .items(new StringProperty()._enum(WorksIncludes.recognisedIncludes.asJava))

  prefix(s"$apiPrefix/${ApiVersions.v1.toString}") {
    setupResultListEndpoint(ApiVersions.v1, ApiV1Swagger,"/works",DisplayWorkV1.apply)
    setupSingleWorkEndpoint(ApiVersions.v1, ApiV1Swagger, "/works/:id",DisplayWorkV1.apply)
  }

  prefix(s"$apiPrefix/${ApiVersions.v2.toString}") {
    setupResultListEndpoint(ApiVersions.v2, ApiV2Swagger, "/works", DisplayWorkV2.apply)
    setupSingleWorkEndpoint(ApiVersions.v2,ApiV2Swagger, "/works/:id",DisplayWorkV2.apply)
  }

  private def setupResultListEndpoint[T <: DisplayWork](version: ApiVersions.Value, swagger: Swagger,
                                      endpointSuffix: String, toDisplayWork: (IdentifiedWork, WorksIncludes) => T)(implicit evidence: TypeTag[DisplayResultList[T]]): Unit = {
    getWithDoc(s"$endpointSuffix", swagger) { doc =>
      setupResultListSwaggerDocs[T](s"$endpointSuffix", swagger, doc)
    } { request: MultipleResultsRequest =>
      val pageSize = request.pageSize.getOrElse(defaultPageSize)
      val includes = request.includes.getOrElse(WorksIncludes())

      for {
        resultList <- getWorkList(request, pageSize)
        displayResultList = DisplayResultList(
          resultList = resultList,
          toDisplayWork,
          pageSize = pageSize,
          includes = includes)
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

  private def setupSingleWorkEndpoint[T <: DisplayWork](version: ApiVersions.Value, swagger: Swagger,
                                      endpointSuffix: String,toDisplayWork: (IdentifiedWork, WorksIncludes) => T)(implicit evidence: TypeTag[T]): Unit = {
    getWithDoc(s"$endpointSuffix", swagger) { doc =>
      setUpSingleWorkSwaggerDocs[T](swagger, doc)
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
        generateSingleWorkResponse(maybeWork, toDisplayWork, includes, request, contextUri)

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

  private def generateSingleWorkResponse[T <: DisplayWork](maybeWork: Option[IdentifiedWork],
                                         toDisplayWork: (IdentifiedWork, WorksIncludes) => T,
                                                           includes: WorksIncludes,
                                         request: SingleWorkRequest,
                                         contextUri: String) =
    maybeWork match {
      case Some(work: IdentifiedWork) =>
        if (work.visible) {
          respondWithWork[T](toDisplayWork(work, includes), contextUri: String)
        } else {
          respondWithGoneError(contextUri: String)
        }
      case None =>
        respondWithNotFoundError(request, contextUri: String)
    }

  private def respondWithWork[T <: DisplayWork](result: T,
                              contextUri: String) = {
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

  private def setupResultListSwaggerDocs[T <: DisplayWork](endpointSuffix: String, swagger: Swagger,
                                         doc: Operation)(implicit evidence: TypeTag[DisplayResultList[T]]) = {
    implicit val finatraSwagger = swagger
    doc
      .summary(endpointSuffix)
      .description("Returns a paginated list of works")
      .tag("Works")
      .responseWith[DisplayResultList[T]](200, "ResultList[Work]")
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

  private def setUpSingleWorkSwaggerDocs[T <: DisplayWork](swagger: Swagger, doc: Operation)(implicit evidence: TypeTag[T])  = {
    implicit val finatraSwagger = swagger
    doc
      .summary(s"/works/{id}")
      .description("Returns a single work")
      .tag("Works")
      .routeParam[String]("id", "The work to return", required = true)
      .responseWith[T](200, "Work")
      .responseWith[DisplayError](400, "Bad Request Error")
      .responseWith[DisplayError](404, "Not Found Error")
      .responseWith[DisplayError](410, "Gone Error")
      .responseWith[DisplayError](500, "Internal Server Error")
      .parameter(includesSwaggerParam)
    // Deliberately undocumented: the index flag.  See above.
  }

  def getWithDoc[RequestType: Manifest, ResponseType: Manifest](route: String, swagger: Swagger, name: String = "", admin: Boolean = false, routeIndex: Option[RouteIndex] = None)
                                                               (doc: Operation => Unit)
                                                               (callback: RequestType => ResponseType): Unit = {
    registerOperation(route, "get", swagger)(doc)
    dsl.get(route, name, admin, routeIndex)(callback)
  }

  private def registerOperation(path: String, method: String, swagger: Swagger)(doc: Operation => Unit): Unit = {
    val op = new Operation
    doc(op)

    FinatraSwagger.convertToFinatraSwagger(swagger).registerOperation(path, method, op)
  }

  implicit def convertToFinatraOperation(operation: Operation): FinatraOperation = new FinatraOperation(operation)
}