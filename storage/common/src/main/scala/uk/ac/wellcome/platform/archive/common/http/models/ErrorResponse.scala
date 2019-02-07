package uk.ac.wellcome.platform.archive.common.http.models

import java.net.URL

import akka.http.scaladsl.model.StatusCode
import io.circe.generic.extras.JsonKey

case class ErrorResponse(
  @JsonKey("@context") context: String,
  httpStatus: Int,
  description: Option[String],
  label: String,
  `type`: String = "Error"
)

case object ErrorResponse {
  def apply(context: URL,
            statusCode: StatusCode,
            description: String): ErrorResponse =
    ErrorResponse(
      context = context.toString,
      httpStatus = statusCode.intValue(),
      description = Some(description),
      label = statusCode.reason()
    )

  def apply(context: URL, statusCode: StatusCode): ErrorResponse =
    ErrorResponse(
      context = context.toString,
      httpStatus = statusCode.intValue(),
      description = None,
      label = statusCode.reason()
    )
}
