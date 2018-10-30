package uk.ac.wellcome.platform.archive.progress_http

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Location
import com.google.inject.Inject
import uk.ac.wellcome.platform.archive.common.config.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.common.models.DisplayIngest
import uk.ac.wellcome.platform.archive.common.progress.models.{Progress, ProgressCreateRequest}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.platform.archive.common.progress.models.ProgressCreateRequest._

class Router @Inject()(monitor: ProgressTracker, config: HttpServerConfig) {

  private def createLocationHeader(progress: Progress) =
    Location(s"${config.externalBaseUrl}/progress/${progress.id}")

  def routes = {
    import akka.http.scaladsl.server.Directives._
    import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
    import uk.ac.wellcome.json.JsonUtil._

    pathPrefix("progress") {
      post {
        entity(as[ProgressCreateRequest]) { progressCreateRequest =>
          onSuccess(monitor.initialise(Progress(progressCreateRequest))) {progress =>
              respondWithHeaders(List(createLocationHeader(progress))) {
                complete(Created -> progress)
              }
          }
        }
      } ~ path(Segment) { id: String =>
        get {
          // TODO add test for what happens if id is not a valid UUID
          monitor.get(UUID.fromString(id)) match {
            case scala.Some(progress) =>
              complete(DisplayIngest(progress))
            case scala.None =>
              complete(NotFound -> "Progress monitor not found!")
          }
        }
      }
    }
  }
}
