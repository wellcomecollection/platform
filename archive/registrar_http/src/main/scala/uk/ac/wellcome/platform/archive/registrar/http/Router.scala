package uk.ac.wellcome.platform.archive.registrar.http

import akka.http.scaladsl.model.StatusCodes._
import com.google.inject.Inject
import io.circe.Printer
import uk.ac.wellcome.platform.archive.common.config.models.HttpServerConfig
import uk.ac.wellcome.platform.archive.registrar.common.models.StorageManifest
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.registrar.http.models.DisplayBag
import uk.ac.wellcome.storage.dynamo._

class Router @Inject()(vhs: VersionedHybridStore[StorageManifest,
                                                 EmptyMetadata,
                                                 ObjectStore[StorageManifest]],
                       config: HttpServerConfig) {

  def routes = {
    import akka.http.scaladsl.server.Directives._
    import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
    implicit val printer = Printer.noSpaces.copy(dropNullValues = true)

    pathPrefix("registrar") {
      path(Segment / Segment) { (space, id) =>
        get {
          onSuccess(vhs.getRecord(s"$space/$id")) {
            case Some(storageManifest) => complete(DisplayBag(storageManifest, config.contextUrl))
            case None                  => complete(NotFound -> "Storage manifest not found!")
          }
        }
      }
    }
  }
}
