package uk.ac.wellcome.platform.api.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

import javax.inject.{Inject, Singleton}

@Singleton
class MainController @Inject()() extends Controller {
  get("/") { request: Request =>
  	response.ok.json(Map("message" -> "success"))
  }
}
