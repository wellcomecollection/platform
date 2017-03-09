package uk.ac.wellcome.platform.api.controllers

import com.github.xiaodongw.swagger.finatra.SwaggerSupport

import com.twitter.inject.annotations.Flag
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

import javax.inject.{Inject, Singleton}

import uk.ac.wellcome.platform.api.ApiSwagger


@Singleton
class CollectionController @Inject()(
  @Flag("api.prefix") apiPrefix: String) extends Controller with SwaggerSupport {

  override implicit protected val swagger = ApiSwagger

  prefix(apiPrefix) {

    getWithDoc("/collections") { doc =>
      doc.summary("/collections")
        .description("Returns a paginated list of collections")
        .tag("Collections")
        .queryParam[Int]("page", "The page to return from the result list", required = false)
        .queryParam[Int]("pageSize", "The number of collections to return per page (default: 10)", required = false)
        .responseWith[Object](200, "ResultList[Collection]")
    } { request: Request =>
      response.notImplemented
    }

    getWithDoc("/collections/:id") { doc =>
      doc.summary("/collections/{id}")
        .description("Returns a single collection")
        .tag("Collections")
        .routeParam[String]("id", "The collection to return",  required = true)
        .responseWith[Object](200, "Collection")
    } { request: Request =>
      response.notImplemented
    }

  }
}
