package uk.ac.wellcome.platform.reindexer.controllers

import javax.inject.{Inject, Singleton}

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import uk.ac.wellcome.platform.reindexer.modules.ReindexModule

@Singleton
class ManagementController @Inject()() extends Controller {
  get("/management/healthcheck") { request: Request =>
    ReindexModule.agent.get() match {
      case "working" => response.ok.json(Map("message" -> "ok"))
      case "success" => response.created.json(Map("message" -> "success"))
      case "failure" => response.internalServerError.json(Map("message" -> "failure"))
      case state => response.internalServerError(s"Unknown ReindexModule state: $state")
    }
  }
}
