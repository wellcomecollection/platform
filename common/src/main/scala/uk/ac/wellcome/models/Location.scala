package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

case class Location(
  locationType: String,
  url: Option[String] = None,
  license: LicenseTrait
) {
  @JsonProperty("type") val ontologyType: String = "Location"
}
