package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

case class Organisation (
  override val label: String,
  @JsonProperty("type") override val ontologyType: String = "Organisation"
) extends Agent(label, ontologyType)
