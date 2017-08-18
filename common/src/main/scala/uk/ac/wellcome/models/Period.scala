package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

case class Period(label: String) {
  @JsonProperty("type") val ontologyType: String = "Period"
}
