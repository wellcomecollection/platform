package uk.ac.wellcome.display.models.v1

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal._

/** Represents an Agent in the API.
  *
  * Note that the V1 API is only used to render Miro works, which:
  *  1. Only use Agent, never the Organisation or Person specialisations
  *  2. Don't have identifiers on instances of Agent
  *
  * So we can assume that we should only ever need to render instances
  * of Unidentifiable[Agent] as a DisplayAgentV1.
  *
  */
@ApiModel(
  value = "Agent"
)
case class DisplayAgentV1(
  @ApiModelProperty(
    value = "The name of the agent"
  ) label: String,
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "Agent"
)

case object DisplayAgentV1 {
  def apply(displayableAgent: Displayable[AbstractAgent]): DisplayAgentV1 =
    displayableAgent match {
      case Unidentifiable(agent) => DisplayAgentV1(label = agent.label)

      // Rather than writing and testing code to tease out the identified
      // bits here, error out -- the nature of the Miro data means we
      // should never hit this in practice.
      case Identified(_, _, _, _) =>
        throw new IllegalArgumentException(
          s"Unexpectedly asked to convert identified agent $displayableAgent to DisplayAgentV1"
        )
    }
}
