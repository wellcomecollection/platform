package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal._

@ApiModel(
  value = "ProductionEvent",
  description =
    "An event contributing to the production, publishing or distribution of a work."
)
case class DisplayProductionEvent(
  @ApiModelProperty label: String,
  @ApiModelProperty places: List[DisplayPlace],
  @ApiModelProperty agents: List[DisplayAbstractAgentV2],
  @ApiModelProperty dates: List[DisplayPeriod],
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.display.models.v2.DisplayAbstractConcept"
  ) function: Option[DisplayAbstractConcept],
  @JsonProperty("type") @JsonKey("type") ontologyType: String =
    "ProductionEvent"
)

object DisplayProductionEvent {
  def apply(productionEvent: ProductionEvent[Displayable[AbstractAgent]],
            includesIdentifiers: Boolean): DisplayProductionEvent = {
    DisplayProductionEvent(
      label = productionEvent.label,
      places = productionEvent.places.map { DisplayPlace(_) },
      agents = productionEvent.agents.map {
        DisplayAbstractAgentV2(_, includesIdentifiers = includesIdentifiers)
      },
      dates = productionEvent.dates.map { DisplayPeriod(_) },
      function = productionEvent.function.map { concept: Concept =>
        DisplayConcept(label = concept.label)
      }
    )
  }
}
