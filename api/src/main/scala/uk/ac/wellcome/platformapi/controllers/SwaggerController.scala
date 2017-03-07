package uk.ac.wellcome.platform.api.controllers

import com.twitter.inject.annotations.Flag
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

import io.swagger.util.Json
import io.swagger.models.Info

import javax.inject.{Inject, Singleton}

import uk.ac.wellcome.platform.api.ApiSwagger


@Singleton
class SwaggerController @Inject()(
  @Flag("api.prefix") apiPrefix: String,
  @Flag("api.version") apiVersion: String) extends Controller {

  prefix(apiPrefix) {

    get("/swagger.json") { request: Request =>
      ApiSwagger.info(
        new Info()
          .description("Search our collections")
          .version(apiVersion)
          .title("Catalogue"))
      response.ok.json(Json.mapper.writeValueAsString(ApiSwagger))
    }

  }
}
