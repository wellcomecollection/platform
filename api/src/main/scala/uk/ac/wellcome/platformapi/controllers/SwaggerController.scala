package uk.ac.wellcome.platform.api.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

import io.swagger.util.Json

import javax.inject.Singleton

import uk.ac.wellcome.platform.api.ApiSwagger


@Singleton
class SwaggerController extends Controller {

  get("/swagger.json") { request: Request =>
    response.ok.json(Json.mapper.writeValueAsString(ApiSwagger))
  }

}
