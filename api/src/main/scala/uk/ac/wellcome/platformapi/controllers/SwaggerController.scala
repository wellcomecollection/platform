package uk.ac.wellcome.platform.api.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

import io.swagger.models.Swagger
import io.swagger.util.Json


//@Singleton
class SwaggerController(swagger: Swagger) extends Controller {

  get("/swagger.json") { request: Request =>
    response.ok.json(Json.mapper.writeValueAsString(swagger))
  }

}
