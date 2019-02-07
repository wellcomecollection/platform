package uk.ac.wellcome.platform.storage.ingests.api.model

import java.net.URL

import akka.http.scaladsl.model.StatusCode
import io.circe.generic.extras.JsonKey

case class ErrorResponse(
  @JsonKey("@context") context: String,
  httpStatus: Int,
  description: String,
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
      description = description,
      label = statusCode.reason()
    )
}
