package uk.ac.wellcome.platform.sierra_reader.controllers

import javax.inject.{Inject, Singleton}

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

@Singleton
class ManagementController @Inject()() extends Controller {
  get("/management/healthcheck") { request: Request =>
    response.ok.json(Map("message" -> "ok"))
  }
}
