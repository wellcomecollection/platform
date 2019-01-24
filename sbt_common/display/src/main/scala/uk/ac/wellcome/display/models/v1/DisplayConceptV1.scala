package uk.ac.wellcome.display.models.v1

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.{AbstractRootConcept, Displayable}

@ApiModel(
  value = "Concept",
  description = "A broad concept"
)
case class DisplayConceptV1(
  @ApiModelProperty(
    dataType = "String"
  ) label: String,
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "Concept"
)

case object DisplayConceptV1 {
  def apply(concept: Displayable[AbstractRootConcept]): DisplayConceptV1 = {
    val label = concept.agent.label
    DisplayConceptV1(label = label)
  }
}
