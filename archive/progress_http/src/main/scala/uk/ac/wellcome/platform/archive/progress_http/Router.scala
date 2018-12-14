package uk.ac.wellcome.platform.archive.progress_http

import java.net.URL
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.{
  MalformedRequestContentRejection,
  RejectionHandler,
  Route
}
import io.circe.{CursorOp, DecodingFailure, Printer}
import uk.ac.wellcome.platform.archive.common.config.models.HTTPServerConfig
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.platform.archive.display.{
  RequestDisplayIngest,
  ResponseDisplayIngest
}
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

  implicit val rejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case MalformedRequestContentRejection(err, cause: DecodingFailure) =>
        val path = CursorOp.opsToPath(cause.history)

        val reason = cause.message match {
          case s if s.contains("failed cursor") =>
            "required property not supplied."
          case s if s.contains("invalid") => s
          case s                          => s"should be a $s."
        }

        val message = s"Invalid value at $path: $reason"

        complete(
          BadRequest -> ErrorResponse(
            BadRequest.intValue,
            message,
            BadRequest.reason))
    }
    .result()

  private def createLocationHeader(progress: Progress) =
    Location(s"${httpServerConfig.externalBaseURL}/${progress.id}")

  def routes: Route = {
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
