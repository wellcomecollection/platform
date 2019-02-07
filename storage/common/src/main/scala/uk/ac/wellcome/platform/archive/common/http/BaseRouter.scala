package uk.ac.wellcome.platform.archive.common.http

import java.net.URL

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import grizzled.slf4j.Logging
import io.circe.{CursorOp, Printer}
import uk.ac.wellcome.platform.archive.common.http.models.ErrorResponse

import scala.concurrent.ExecutionContext

trait BaseRouter extends Logging {
  val contextURL: URL
  val httpMetrics: HttpMetrics

  def routes: Route

  import akka.http.scaladsl.server.Directives._
  import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
  import uk.ac.wellcome.json.JsonUtil._

  implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)
  implicit val ec: ExecutionContext

  val sendCloudWatchMetrics: Directive0 = mapResponse { resp: HttpResponse =>
    httpMetrics.sendMetric(resp)
    resp
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
      .mapRejectionResponse { resp: HttpResponse =>
        httpMetrics.sendMetric(resp)
        resp
      }

  implicit def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case err: Exception =>
      logger.error(s"Unexpected exception $err")
      val error = ErrorResponse(
        context = contextURL,
        statusCode = InternalServerError,
        description = err.toString
      )
      httpMetrics.sendMetricForStatus(InternalServerError)
      complete(InternalServerError -> error)
  }

  private def handleDecodingFailures(causes: DecodingFailures): StandardRoute = {
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
        context = contextURL,
        statusCode = BadRequest,
        description = message.toList.mkString("\n")
      )
    )
  }

  private def transformToJsonErrorResponse(
    statusCode: StatusCode,
    res: HttpResponse): HttpResponse = {
    val errorResponseMarshallingFlow = Flow[ByteString]
      .mapAsync(parallelism = 1)(data => {
        val message = data.utf8String
        Marshal(
          ErrorResponse(
            context = contextURL,
            statusCode = statusCode,
            description = message)).to[MessageEntity]
      })
      .flatMapConcat(_.dataBytes)

    res
      .transformEntityDataBytes(errorResponseMarshallingFlow)
      .mapEntity(entity =>
        entity.withContentType(ContentTypes.`application/json`))
  }
}
