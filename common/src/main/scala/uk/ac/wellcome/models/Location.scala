package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

case class Location(
  locationType: String,
  url: Option[String] = None,
  license: License
) {
  @JsonProperty("type") val ldType: String = "Location"
}
