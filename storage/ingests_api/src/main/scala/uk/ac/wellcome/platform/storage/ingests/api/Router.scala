package uk.ac.wellcome.platform.storage.ingests.api

import java.net.URL
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server._
import grizzled.slf4j.Logging
import io.circe.Printer
import uk.ac.wellcome.platform.archive.common.config.models.HTTPServerConfig
import uk.ac.wellcome.platform.archive.common.http.models.ErrorResponse
import uk.ac.wellcome.platform.archive.common.models.StorageSpace
import uk.ac.wellcome.platform.archive.common.models.bagit.{
  BagId,
  ExternalIdentifier
}
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.platform.archive.display.{
  DisplayIngestMinimal,
  RequestDisplayIngest,
  ResponseDisplayIngest
}

class Router(progressTracker: ProgressTracker,
             progressStarter: ProgressStarter,
             httpServerConfig: HTTPServerConfig,
             contextURL: URL)
    extends Logging {

  import akka.http.scaladsl.server.Directives._
  import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
  import uk.ac.wellcome.json.JsonUtil._

  implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

  def routes: Route =
    pathPrefix("progress") {
      post {
        entity(as[RequestDisplayIngest]) { requestDisplayIngest =>
          onSuccess(progressStarter.initialise(requestDisplayIngest.toProgress)) {
            progress =>
              respondWithHeaders(List(createLocationHeader(progress))) {
                complete(Created -> ResponseDisplayIngest(progress, contextURL))
              }
          }
        }
      } ~ path(JavaUUID) { id: UUID =>
        get {
          onSuccess(progressTracker.get(id)) {
            case Some(progress) =>
              complete(ResponseDisplayIngest(progress, contextURL))
            case None =>
              complete(NotFound -> "Progress monitor not found!")
          }
        }
      } ~ path("find-by-bag-id" / Segment) { combinedId: String =>
        // Temporary route to match colon separated ids '/find-by-bag-id/storageSpace:bagId' used by DLCS
        // remove when DLCS replaces this by '/find-by-bag-id/storageSpace/bagId'
        get {
          val parts = combinedId.split(':')
          val bagId =
            BagId(StorageSpace(parts.head), ExternalIdentifier(parts.last))
          findProgress(bagId)
        }
      } ~ path("find-by-bag-id" / Segment / Segment) { (space, id) =>
        // Route used by DLCS to find ingests for a bag, not part of the public/documented API.  Either remove
        // if no longer needed after migration or enhance and document as part of the API.
        get {
          val bagId = BagId(StorageSpace(space), ExternalIdentifier(id))
          findProgress(bagId)
        }
      }
    }

  private def findProgress(bagId: BagId) = {
    val results = progressTracker.findByBagId(bagId)
    if (results.nonEmpty && results.forall(_.isRight)) {
      complete(OK -> results.collect {
        case Right(bagProgress) => DisplayIngestMinimal(bagProgress)
      })
    } else if (results.isEmpty) {
      complete(NotFound -> List[DisplayIngestMinimal]())
    } else {
      info(s"""errors fetching ingests for $bagId: ${results.mkString(" ")}""")
      complete(
        InternalServerError -> ErrorResponse(
          context = contextURL,
          statusCode = InternalServerError,
          description = InternalServerError.reason
        ))
    }
  }

  private def createLocationHeader(progress: Progress) =
    Location(s"${httpServerConfig.externalBaseURL}/${progress.id}")
}
