package uk.ac.wellcome.platform.archive.progress_http.model

import io.circe.generic.extras.JsonKey

case class ErrorResponse(@JsonKey("@context")
                         context: String,
                         httpStatus: Int,
                         description: String,
                         label: String,
                         `type`: String = "Error")
