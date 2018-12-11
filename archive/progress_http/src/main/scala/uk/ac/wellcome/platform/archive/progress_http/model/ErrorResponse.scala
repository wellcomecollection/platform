package uk.ac.wellcome.platform.archive.progress_http.model

case class ErrorResponse(httpStatus: Int, description: String, label: String, `type`: String = "Error")
