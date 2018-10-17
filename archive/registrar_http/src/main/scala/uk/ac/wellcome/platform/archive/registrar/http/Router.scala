package uk.ac.wellcome.platform.archive.registrar.http

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes._
import com.google.inject.Inject
import uk.ac.wellcome.platform.archive.common.config.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.common.models.DisplayIngest
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor

class Router @Inject()(monitor: ProgressMonitor, config: HttpServerConfig) {

  def routes = {
    import akka.http.scaladsl.server.Directives._
    import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
    import uk.ac.wellcome.json.JsonUtil._

    pathPrefix("registrar") {
      path(Segment) { id: String =>
        get {
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
