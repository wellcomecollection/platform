package uk.ac.wellcome.finatra.controllers

import javax.inject.Singleton

import com.google.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

@Singleton
class ManagementController @Inject()() extends Controller {
  val appName = scala.util.Properties.envOrElse("APP_NAME", "not-specified")

  get("/management/healthcheck") { request: Request =>
    response.ok.json(Map("message" -> "ok"))
  }

  get("/management/manifest") { request: Request =>
    response.ok.json(Map("name" -> appName))
  }
}
