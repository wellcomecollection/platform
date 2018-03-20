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

@ApiModel(
  value = "Person"
)
case class DisplayPerson(
                          @ApiModelProperty(
  value = "The name of the person"
) label: String,
                          @ApiModelProperty(
  dataType = "String",
  value =
    "The title of the person"
)prefix: Option[String]= None,
                          @ApiModelProperty(
  dataType = "String",
  value =
    "The numerationof the person"
)numeration: Option[String]= None,
                          @JsonProperty("type") ontologyType: String = "Person") extends DisplayAbstractAgent

@ApiModel(
  value = "Organisation"
)
case class DisplayOrganisation(@ApiModelProperty(
  value = "The name of the organisation"
)label: String, @JsonProperty("type") ontologyType: String = "Organisation") extends DisplayAbstractAgent