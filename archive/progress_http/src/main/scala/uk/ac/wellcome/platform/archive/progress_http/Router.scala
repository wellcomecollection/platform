package uk.ac.wellcome.platform.archive.progress_http

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Location
import com.google.inject.Inject
import io.circe.Printer
import uk.ac.wellcome.platform.archive.common.config.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.common.models.{
  RequestDisplayIngest,
  ResponseDisplayIngest
}
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker

class Router @Inject()(monitor: ProgressTracker,
                       progressStarter: ProgressStarter,
                       config: HttpServerConfig) {

  private def createLocationHeader(progress: Progress) =
    Location(s"${config.externalBaseUrl}/${progress.id}")

  def routes = {
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
                  Created -> ResponseDisplayIngest(progress, config.contextUrl))
              }
          }
        }
      } ~ path(JavaUUID) { id: UUID =>
        get {
          onSuccess(monitor.get(id)) {
            case Some(progress) =>
              complete(ResponseDisplayIngest(progress, config.contextUrl))
            case None =>
              complete(NotFound -> "Progress monitor not found!")
          }
        }
      }
    }
  }
}
