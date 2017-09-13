package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

case class Error(
  errorType: String,
  httpStatus: Option[Int],
  label: String,
  description: Option[String]
) {
  @JsonProperty("type") val ontologyType: String = "Error"
}

case object Error {
  def apply(variant: String, description: String): Error = {
    variant match {
      case "http-404" => Error(
        errorType = "http",
        httpStatus = Some(404),
        label = "Not Found",
        description = Some(description)
      )
      case unknownVariant =>
        throw new Exception(s"$unknownVariant is not a valid error variant")
    }
  }
}
