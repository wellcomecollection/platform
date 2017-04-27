package uk.ac.wellcome.platform.api.controllers

import javax.inject.{Inject, Singleton}

import com.github.xiaodongw.swagger.finatra.SwaggerSupport
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.inject.annotations.Flag
import uk.ac.wellcome.platform.api.ApiSwagger
import uk.ac.wellcome.platform.api.responses.{
  ResultListResponse,
  ResultResponse
}
import uk.ac.wellcome.platform.api.services.ElasticSearchService
import uk.ac.wellcome.platform.api.utils.ApiRequestUtils
import uk.ac.wellcome.utils.GlobalExecutionContext.context

@Singleton
class WorksController @Inject()(
                                 @Flag("api.prefix") apiPrefix: String,
                                 @Flag("api.context") apiContext: String,
                                 elasticService: ElasticSearchService
) extends Controller
    with SwaggerSupport {

  override implicit protected val swagger = ApiSwagger

  prefix(apiPrefix) {
    // This is a demo endpoint for the UX team to use when prototyping
    // item pages.
    // TODO: Remove this endpoint.
    get(s"/demoItem") { request: Request =>
      response.ok.json(
        Map(
          "@context" -> "http://id.wellcomecollection.org/",
          "id" -> "cbsx6cvr",
          "type" -> "item",
          "title" -> "The natural history of monkeys",
          "date" -> "1546-04-07",
          "authors" -> Array("William Jardine"),
          "description" -> "230 page, color plates : frontispiece (portrait), add. color t.page ; (8vo)",
          "topics" -> Array("monkeys", "animals"),
          "media" -> Array("https://wellcomelibrary.org/content/59301/60865")
        ))
    }

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
      elasticService
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
      elasticService
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
