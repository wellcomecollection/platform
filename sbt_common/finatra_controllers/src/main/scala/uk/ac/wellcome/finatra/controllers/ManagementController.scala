package uk.ac.wellcome.finatra.controllers

import com.google.inject.{Inject, Singleton}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

@Singleton
class ManagementController @Inject()() extends Controller {
  val appName = scala.util.Properties.envOrElse("APP_NAME", "not-specified")

  get("/management/healthcheck") { _: Request =>
    response.ok.body(Map("message" -> "ok"))
  }

  get("/management/manifest") { _: Request =>
    response.ok.json(Map("name" -> appName))
  }
}
