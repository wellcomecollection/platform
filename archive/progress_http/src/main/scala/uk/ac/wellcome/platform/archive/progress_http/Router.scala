package uk.ac.wellcome.platform.archive.progress_http

import akka.http.scaladsl.model.StatusCodes._
import com.google.inject.Inject
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.common.json.{URIConverters, UUIDConverters}
import uk.ac.wellcome.platform.archive.common.modules.HttpServerConfig
import uk.ac.wellcome.platform.archive.common.progress.models.{Progress, ProgressCreateRequest}
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor

import scala.util.Try

class Router @Inject()(monitor: ProgressMonitor, config: HttpServerConfig) extends URIConverters with UUIDConverters {

  def routes = {
    import akka.http.scaladsl.server.Directives._
    import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

    pathPrefix("progress") {
      post {
        entity(as[ProgressCreateRequest]) { progressCreateRequest =>
          Try(monitor.create(Progress(progressCreateRequest))) match {
            case util.Success(progress) => complete(Created -> progress)
            case util.Failure(e) => failWith(e)
          }
        }
      } ~ path(JavaUUID) { uuid =>
        get {
          monitor.get(uuid) match {
            case scala.Some(progress) => complete(progress)
            case scala.None =>
              complete(NotFound -> "Progress monitor not found!")
          }
        }
      }
    }
  }
}

