package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

case class Concept(
  label: String
) {
  @JsonProperty("type") val ontologyType: String = "Concept"
}
