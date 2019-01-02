package uk.ac.wellcome.platform.archive.progress_http

import java.net.URL
import java.util.UUID

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Location
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import akka.http.scaladsl.server.{
  MalformedRequestContentRejection,
  RejectionHandler,
  Route
}
import grizzled.slf4j.Logging
import io.circe.{CursorOp, Printer}
import uk.ac.wellcome.platform.archive.common.config.models.HTTPServerConfig
import uk.ac.wellcome.platform.archive.common.models.{
  BagId,
  ExternalIdentifier,
  StorageSpace
}
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressTracker
import uk.ac.wellcome.platform.archive.display.{
  DisplayIngestMinimal,
  RequestDisplayIngest,
  ResponseDisplayIngest
}
import uk.ac.wellcome.platform.archive.progress_http.model.ErrorResponse

import scala.concurrent.ExecutionContext

class Router(progressTracker: ProgressTracker,
             progressStarter: ProgressStarter,
             httpServerConfig: HTTPServerConfig,
             contextURL: URL)(implicit ec: ExecutionContext)
    extends Logging {

  import akka.http.scaladsl.server.Directives._
  import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
  import uk.ac.wellcome.json.JsonUtil._
  implicit val printer = Printer.noSpaces.copy(dropNullValues = true)
  import uk.ac.wellcome.platform.archive.display.DisplayProvider._

  def routes: Route = pathPrefix("progress") {
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
          context = contextURL.toString,
          httpStatus = InternalServerError.intValue,
          description = "Internal server error",
          label = InternalServerError.reason
        ))
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
