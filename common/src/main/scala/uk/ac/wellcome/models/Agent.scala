package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

case class Agent(label: String) {
  @JsonProperty("type") val ontologyType: String = "Agent"
}
