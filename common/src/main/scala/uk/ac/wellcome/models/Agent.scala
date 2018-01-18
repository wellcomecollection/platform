package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

sealed trait AbstractAgent {
  val label: String
  @JsonProperty("type") val ontologyType: String
}

case class Agent(label:String, ontologyType: String = "Agent") extends AbstractAgent

case class Organisation (
                          override val label: String,
                          @JsonProperty("type") override val ontologyType: String = "Organisation"
                        ) extends AbstractAgent