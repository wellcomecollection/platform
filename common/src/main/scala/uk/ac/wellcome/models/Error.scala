package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

case class Error(
  errorType: String,
  httpStatus: Option[Int],
  label: String,
  description: Option[String],
  reference: Option[String]
) {
  @JsonProperty("type") val ontologyType: String = "Error"
}
