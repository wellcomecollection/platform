package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

case class Location(
  locationType: String,
  url: Option[String] = None,
  credit: Option[String] = None,
  license: License,
    @JsonProperty("type") ontologyType: String = "Location"
)
