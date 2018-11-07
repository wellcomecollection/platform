package uk.ac.wellcome.platform.archive.progress_http

import java.net.URL
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import io.circe.Printer
import uk.ac.wellcome.platform.archive.common.config.models.{HTTPServerConfig, HttpServerConfig}
import uk.ac.wellcome.platform.archive.common.models.{RequestDisplayIngest, ResponseDisplayIngest}
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker

class Router (
  monitor: ProgressTracker,
  progressStarter: ProgressStarter,
  httpServerConfig: HTTPServerConfig,
  contextURL: URL
) {

  private def createLocationHeader(progress: Progress) =
    Location(s"${httpServerConfig.externalBaseURL}/${progress.id}")

  def routes: Route = {
    import akka.http.scaladsl.server.Directives._
    import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
    import uk.ac.wellcome.json.JsonUtil._
    implicit val printer = Printer.noSpaces.copy(dropNullValues = true)

    pathPrefix("progress") {
      post {
        entity(as[RequestDisplayIngest]) { progressCreateRequest =>
          onSuccess(progressStarter.initialise(Progress(progressCreateRequest))) {
            progress =>
              respondWithHeaders(List(createLocationHeader(progress))) {
                complete(
                  Created -> ResponseDisplayIngest(progress, contextURL))
              }
          }
        }
      } ~ path(JavaUUID) { id: UUID =>
        get {
          onSuccess(monitor.get(id)) {
            case Some(progress) =>
              complete(ResponseDisplayIngest(progress, contextURL))
            case None =>
              complete(NotFound -> "Progress monitor not found!")
          }
        }
      }
    }
  }
}
