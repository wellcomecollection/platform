package uk.ac.wellcome.platform.storage.bags.api

import java.net.URL

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import io.circe.Printer
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archive.registrar.common.models.StorageManifest
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.platform.storage.bags.api.models.DisplayBag
import uk.ac.wellcome.storage.dynamo._

import scala.concurrent.ExecutionContext

class Router(vhs: VersionedHybridStore[StorageManifest,
                                       EmptyMetadata,
                                       ObjectStore[StorageManifest]],
             contextURL: URL)(implicit val ec: ExecutionContext) {

  def routes: Route = {
    import akka.http.scaladsl.server.Directives._
    import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

    pathPrefix("registrar") {
      path(Segment / Segment) { (space, id) =>
        get {
          onSuccess(vhs.getRecord(s"$space/$id")) {
            case Some(storageManifest) =>
              complete(DisplayBag(storageManifest, contextURL))
            case None => complete(NotFound -> "Storage manifest not found!")
          }
        }
      }
    }
  }
}
