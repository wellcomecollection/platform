package uk.ac.wellcome.finatra.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import javax.inject.{Inject, Singleton}

@Singleton
class ManagementController @Inject()() extends Controller {
  get("/management/healthcheck") { request: Request =>
    response.ok.json(Map("message" -> "ok"))
  }
}
