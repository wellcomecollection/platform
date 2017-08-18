package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

case class License(
  val licenseType: String,
  val label: String,
  val url: String
) {
  @JsonProperty("type") val ontologyType: String = "License"
}
