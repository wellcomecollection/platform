package uk.ac.wellcome.models.display

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.Concept

@ApiModel(
  value = "Concept",
  description = "A broad concept"
)
case class DisplayConcept(
  @ApiModelProperty(
    dataType = "String"
  ) label: String
) {
  @JsonProperty("type") val ontologyType: String = "Concept"
}

case object DisplayConcept {
  def apply(concept: Concept): DisplayConcept = DisplayConcept(
    label = concept.label
  )
}
