package uk.ac.wellcome.platform.archive.progress_http

import java.net.URL
import java.util.UUID

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.{
  MalformedRequestContentRejection,
  RejectionHandler,
  Route
}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import io.circe.{CursorOp, Printer}
import uk.ac.wellcome.platform.archive.common.config.models.HTTPServerConfig
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.platform.archive.display.{
  RequestDisplayIngest,
  ResponseDisplayIngest
}
import uk.ac.wellcome.platform.archive.progress_http.model.ErrorResponse

import scala.concurrent.ExecutionContext

class Router(
  monitor: ProgressTracker,
  progressStarter: ProgressStarter,
  httpServerConfig: HTTPServerConfig,
  contextURL: URL
)(implicit ec: ExecutionContext) {

  import akka.http.scaladsl.server.Directives._
  import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
  import uk.ac.wellcome.json.JsonUtil._
  implicit val printer = Printer.noSpaces.copy(dropNullValues = true)
  import uk.ac.wellcome.platform.archive.display.DisplayProvider._

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

  def rejectionHandler: RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        case MalformedRequestContentRejection(_, causes: DecodingFailures) =>
          handleDecodingFailures(causes)
      }
      .result()
      .seal
      .mapRejectionResponse {
        case res @ HttpResponse(
              statusCode,
              _,
              HttpEntity.Strict(contentType, _),
              _) if contentType != ContentTypes.`application/json` =>
          transformToJsonErrorResponse(statusCode, res)
        case x => x
      }

  private def createLocationHeader(progress: Progress) =
    Location(s"${httpServerConfig.externalBaseURL}/${progress.id}")

  private def handleDecodingFailures(causes: DecodingFailures) = {
    val message = causes.failures.map { cause =>
      val path = CursorOp.opsToPath(cause.history)

      // Error messages returned by Circe are somewhat inconsistent and we also return our
      // own error messages when decoding enums (DisplayIngestType and DisplayStorageProvider).
      val reason = cause.message match {
        // "Attempt to decode value on failed cursor" seems to mean in circeworld
        // that a required field was not present.
        case s if s.contains("Attempt to decode value on failed cursor") =>
          "required property not supplied."
        // These are errors returned by our custom decoders for enum.
        case s if s.contains("valid values") => s
        // If a field exists in the JSON but it's of the wrong format
        // (for example the schema says it should be a String but an object has
        // been supplied instead), the error message returned by Circe only
        // contains the expected type.
        case s => s"should be a $s."
      }

      s"Invalid value at $path: $reason"
    }

    complete(
      BadRequest -> ErrorResponse(
        contextURL.toString,
        BadRequest.intValue,
        message.toList.mkString("\n"),
        BadRequest.reason))
  }

  private def transformToJsonErrorResponse(statusCode: StatusCode,
                                           res: HttpResponse) = {
    val errorResponseMarshallingFlow = Flow[ByteString]
      .mapAsync(1)(data => {
        val message = data.utf8String
        Marshal(
          ErrorResponse(
            context = contextURL.toString,
            httpStatus = statusCode.intValue,
            description = message,
            label = statusCode.reason)).to[MessageEntity]
      })
      .flatMapConcat(_.dataBytes)

    res
      .transformEntityDataBytes(errorResponseMarshallingFlow)
      .mapEntity(entity =>
        entity.withContentType(ContentTypes.`application/json`))
  }
}
