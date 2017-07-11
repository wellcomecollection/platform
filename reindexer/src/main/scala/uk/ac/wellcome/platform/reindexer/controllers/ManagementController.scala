package uk.ac.wellcome.platform.reindexer.controllers

import javax.inject.{Inject, Singleton}

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import uk.ac.wellcome.platform.reindexer.models.{JobStatus, ReindexStatus}

@Singleton
class ManagementController @Inject()() extends Controller {
  get("/management/healthcheck") { request: Request =>
    val currentStatus = ReindexStatus.currentStatus

    val respond = currentStatus match {
      case ReindexStatus(JobStatus.Init, _, _) => response.ok.json _
      case ReindexStatus(JobStatus.Working, _, _) => response.ok.json _
      case ReindexStatus(JobStatus.Success, _, _) => response.created.json _
      case ReindexStatus(JobStatus.Failure, _, _) =>
        response.internalServerError.json _
    }

    respond(currentStatus.toMap)
  }
}
