package uk.ac.wellcome.platform.archive.registrar.http

import akka.http.scaladsl.model.StatusCodes._
import com.google.inject.Inject
import uk.ac.wellcome.platform.archive.common.config.models.HttpServerConfig

class Router @Inject()(config: HttpServerConfig) {

  def routes = {
    import akka.http.scaladsl.server.Directives._
    import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

    pathPrefix("registrar") {
      path(Segment) { id: String =>
        get {

          complete(NotFound -> "Storage manifest not found!")

        }
      }
    }
  }
}
