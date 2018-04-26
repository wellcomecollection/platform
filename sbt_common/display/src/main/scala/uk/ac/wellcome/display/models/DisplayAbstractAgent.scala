package uk.ac.wellcome.display.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.work_model._

@ApiModel(
  value = "Agent"
)
sealed trait DisplayAbstractAgent

@ApiModel(
  value = "Agent"
)
case class DisplayAgent(
  id: Option[String],
  identifiers: Option[List[DisplayIdentifier]],
  @ApiModelProperty(
    value = "The name of the agent"
  ) label: String,
  @JsonProperty("type") ontologyType: String = "Agent"
) extends DisplayAbstractAgent

case object DisplayAbstractAgent {
  def apply(
    displayableAgent: Displayable[AbstractAgent]): DisplayAbstractAgent =
    displayableAgent match {
      case Unidentifiable(a: Agent) =>
        DisplayAgent(
          id = None,
          identifiers = None,
          label = a.label
        )
      case Identified(a: Agent, id, identifiers) =>
        DisplayAgent(
          id = Some(id),
          identifiers = Some(identifiers.map(DisplayIdentifier(_))),
          label = a.label
        )
      case Identified(p: Person, id, identifiers) =>
        DisplayPerson(
          id = Some(id),
          identifiers = Some(identifiers.map(DisplayIdentifier(_))),
          label = p.label,
          prefix = p.prefix,
          numeration = p.numeration)
      case Unidentifiable(p: Person) =>
        DisplayPerson(
          id = None,
          identifiers = None,
          label = p.label,
          prefix = p.prefix,
          numeration = p.numeration)
      case Identified(o: Organisation, id, identifiers) =>
        DisplayOrganisation(
          id = Some(id),
          identifiers = Some(identifiers.map(DisplayIdentifier(_))),
          o.label)
      case Unidentifiable(o: Organisation) =>
        DisplayOrganisation(id = None, identifiers = None, label = o.label)
    }
}

@ApiModel(
  value = "Person"
)
case class DisplayPerson(
  @ApiModelProperty(
    dataType = "String",
    readOnly = true,
    value = "The canonical identifier given to a thing.") id: Option[String],
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.DisplayIdentifier]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifier]],
  @ApiModelProperty(
    value = "The name of the person"
  ) label: String,
  @ApiModelProperty(
    dataType = "String",
    value = "The title of the person"
  ) prefix: Option[String] = None,
  @ApiModelProperty(
    dataType = "String",
    value = "The numeration of the person"
  ) numeration: Option[String] = None,
  @JsonProperty("type") ontologyType: String = "Person")
    extends DisplayAbstractAgent

@ApiModel(
  value = "Organisation"
)
case class DisplayOrganisation(
  @ApiModelProperty(
    dataType = "String",
    readOnly = true,
    value = "The canonical identifier given to a thing.") id: Option[String],
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.DisplayIdentifier]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifier]],
  @ApiModelProperty(
    value = "The name of the organisation"
  ) label: String,
  @JsonProperty("type") ontologyType: String = "Organisation")
    extends DisplayAbstractAgent
