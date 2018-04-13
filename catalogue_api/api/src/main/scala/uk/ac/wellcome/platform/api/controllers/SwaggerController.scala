package uk.ac.wellcome.platform.api.controllers

import com.twitter.inject.annotations.Flag
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

import io.swagger.util.Json
import io.swagger.models.{Info, Scheme}

import javax.inject.{Inject, Singleton}

import uk.ac.wellcome.platform.api.ApiSwagger

@Singleton
class SwaggerController @Inject()(
  @Flag("api.scheme") apiScheme: String,
  @Flag("api.prefix") apiPrefix: String,
  @Flag("api.host") apiHost: String
) extends Controller {

  val scheme = apiScheme match {
    case "https" => Scheme.HTTPS
    case _ => Scheme.HTTP
  }

  prefix(apiPrefix) {
    setupSwaggerEndpoint("v1")
    setupSwaggerEndpoint("v2")
  }

  private def setupSwaggerEndpoint(version: String): Unit = {
    get(s"/$version/swagger.json") { request: Request =>
      ApiSwagger.info(
        new Info()
          .description("Search our collections")
          .version(version)
          .title("Catalogue"))
      ApiSwagger.scheme(scheme)
      ApiSwagger.host(apiHost)
      ApiSwagger.basePath(s"$apiPrefix/$version")
      ApiSwagger.produces("application/json")
      ApiSwagger.produces("application/ld+json")

      response.ok.json(Json.mapper.writeValueAsString(ApiSwagger))
    }
  }
}
