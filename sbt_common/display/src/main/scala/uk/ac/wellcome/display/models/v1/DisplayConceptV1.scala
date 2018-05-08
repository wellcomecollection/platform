package uk.ac.wellcome.display.models.v1

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.{
  AbstractConcept,
  Displayable,
  Identified,
  Unidentifiable
}

@ApiModel(
  value = "Concept",
  description = "A broad concept"
)
case class DisplayConceptV1(
  @ApiModelProperty(
    dataType = "String"
  ) label: String
) {
  @JsonProperty("type") val ontologyType: String = "Concept"
}

case object DisplayConceptV1 {
  def apply(concept: Displayable[AbstractConcept]): DisplayConceptV1 = {
    val label = concept match {
      case Identified(c: AbstractConcept, _, _) => c.label
      case Unidentifiable(c: AbstractConcept) => c.label
    }
    DisplayConceptV1(label = label)
  }
}
