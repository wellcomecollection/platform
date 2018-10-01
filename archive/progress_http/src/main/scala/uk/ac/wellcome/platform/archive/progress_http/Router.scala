package uk.ac.wellcome.platform.archive.progress_http

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Location
import com.google.inject.Inject
import uk.ac.wellcome.platform.archive.common.progress.models.{
  Progress,
  ProgressCreateRequest
}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor
import uk.ac.wellcome.platform.archive.progress_http.models.HttpServerConfig

import scala.util.Try

class Router @Inject()(monitor: ProgressMonitor, config: HttpServerConfig) {

  private def createLocationHeader(progress: Progress) =
    Location(s"${config.externalBaseUrl}/progress/${progress.id}")

  def routes = {
    import akka.http.scaladsl.server.Directives._
    import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
    import io.circe.generic.auto._

    pathPrefix("progress") {
      post {
        entity(as[ProgressCreateRequest]) { progressCreateRequest =>
          Try(monitor.create(Progress(progressCreateRequest))) match {
            case util.Success(progress) => {
              respondWithHeaders(List(createLocationHeader(progress))) {
                complete(Created -> progress)
              }
            }
            case util.Failure(e) => failWith(e)
          }
        }
      } ~ path(Segment) { id: String =>
        get {
          monitor.get(id) match {
            case Some(progress) => complete(progress)
            case None           => complete(NotFound -> "Progress monitor not found!")
          }
        }
      }
    }
  }
}
