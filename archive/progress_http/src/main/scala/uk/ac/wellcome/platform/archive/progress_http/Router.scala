package uk.ac.wellcome.platform.archive.progress_http

import java.net.URL
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.{MalformedRequestContentRejection, RejectionHandler, Route}
import io.circe.CursorOp.DownField
import io.circe.{DecodingFailure, Printer}
import uk.ac.wellcome.platform.archive.common.config.models.HTTPServerConfig
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.platform.archive.display.{RequestDisplayIngest, ResponseDisplayIngest}
import uk.ac.wellcome.platform.archive.progress_http.model.ErrorResponse

class Router(
  monitor: ProgressTracker,
  progressStarter: ProgressStarter,
  httpServerConfig: HTTPServerConfig,
  contextURL: URL
) {

  import akka.http.scaladsl.server.Directives._
  import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
  import uk.ac.wellcome.json.JsonUtil._
  implicit val printer = Printer.noSpaces.copy(dropNullValues = true)
  import uk.ac.wellcome.platform.archive.display.DisplayProvider._

  implicit val rejectionHandler = RejectionHandler.newBuilder()
    .handle { case MalformedRequestContentRejection(_, cause: DecodingFailure) =>
    val keys = cause.history.map{ op => op.asInstanceOf[DownField].k}

    val message = keys match {
      case List(key) => s"Required property $key not supplied"
      case _ => s"Required property ${keys.reduce((key1, key2) => s"$key1 not supplied on $key2")}"
    }

    complete(BadRequest -> ErrorResponse(BadRequest.intValue, message, BadRequest.reason))
  }.result()

  private def createLocationHeader(progress: Progress) =
    Location(s"${httpServerConfig.externalBaseURL}/${progress.id}")

  def routes: Route = {
    pathPrefix("progress") {
      post {
        entity(as[RequestDisplayIngest]) { requestDisplayIngest =>
            onSuccess(
              progressStarter.initialise(requestDisplayIngest.toProgress)) {
              progress =>
                respondWithHeaders(List(createLocationHeader(progress))) {
                  complete(Created -> ResponseDisplayIngest(progress, contextURL))
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
