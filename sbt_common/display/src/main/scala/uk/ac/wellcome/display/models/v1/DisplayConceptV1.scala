package uk.ac.wellcome.display.models.v1

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal._

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
    val label = concept match {
      case Identified(c: AbstractRootConcept, _, _, _) => c.label
      case Unidentifiable(c: AbstractRootConcept)      => c.label
    }
    DisplayConceptV1(label = label)
  }
}
