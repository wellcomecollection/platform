package uk.ac.wellcome.display.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.{AbstractAgent, Agent, Organisation, Person}

sealed trait DisplayAbstractAgent

@ApiModel(
  value = "Agent"
)
case class DisplayAgent(
  @ApiModelProperty(
    value = "The name of the agent"
  ) label: String,
  @JsonProperty("type") ontologyType: String = "Agent"
) extends DisplayAbstractAgent

case object DisplayAbstractAgent {
  def apply(agent: AbstractAgent): DisplayAbstractAgent =
    agent match {
      case a: Agent => DisplayAgent (
    label = a.label,
    ontologyType = a.ontologyType
    )
      case p: Person => DisplayPerson(p.label, p.prefix, p.numeration, p.ontologyType)
      case o: Organisation => DisplayOrganisation(o.label, o.ontologyType)

    }
}

case class DisplayPerson(label: String, prefix: Option[String]= None, numeration: Option[String]= None, ontologyType: String = "Person") extends DisplayAbstractAgent
case class DisplayOrganisation(label: String, ontologyType: String = "Organisation") extends DisplayAbstractAgent