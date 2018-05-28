package uk.ac.wellcome.display.models.v1

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal._

@ApiModel(
  value = "Agent"
)
sealed trait DisplayAbstractAgentV1 {
  val id: Option[String]
  val identifiers: Option[List[DisplayIdentifierV1]]
  val label: String
}

@ApiModel(
  value = "Agent"
)
case class DisplayAgentV1(
  id: Option[String],
  identifiers: Option[List[DisplayIdentifierV1]],
  @ApiModelProperty(
    value = "The name of the agent"
  ) label: String,
  @JsonProperty("type") ontologyType: String = "Agent"
) extends DisplayAbstractAgentV1

case object DisplayAbstractAgentV1 {
  def apply(displayableAgent: Displayable[AbstractAgent],
            includesIdentifiers: Boolean): DisplayAbstractAgentV1 =
    displayableAgent match {
      case Unidentifiable(a: Agent) =>
        DisplayAgentV1(
          id = None,
          identifiers = None,
          label = a.label
        )
      case Identified(a: Agent, id, identifiers) =>
        DisplayAgentV1(
          id = Some(id),
          identifiers =
            if (includesIdentifiers)
              Some(identifiers.map(DisplayIdentifierV1(_)))
            else None,
          label = a.label
        )
      case Identified(p: Person, id, identifiers) =>
        DisplayPersonV1(
          id = Some(id),
          identifiers =
            if (includesIdentifiers)
              Some(identifiers.map(DisplayIdentifierV1(_)))
            else None,
          label = p.label,
          prefix = p.prefix,
          numeration = p.numeration
        )
      case Unidentifiable(p: Person) =>
        DisplayPersonV1(
          id = None,
          identifiers = None,
          label = p.label,
          prefix = p.prefix,
          numeration = p.numeration)
      case Identified(o: Organisation, id, identifiers) =>
        DisplayOrganisationV1(
          id = Some(id),
          identifiers =
            if (includesIdentifiers)
              Some(identifiers.map(DisplayIdentifierV1(_)))
            else None,
          o.label)
      case Unidentifiable(o: Organisation) =>
        DisplayOrganisationV1(id = None, identifiers = None, label = o.label)
    }
}

@ApiModel(
  value = "Person"
)
case class DisplayPersonV1(
  @ApiModelProperty(
    dataType = "String",
    readOnly = true,
    value = "The canonical identifier given to a thing.") id: Option[String],
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.v1.DisplayIdentifierV1]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifierV1]],
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
    extends DisplayAbstractAgentV1

@ApiModel(
  value = "Organisation"
)
case class DisplayOrganisationV1(
  @ApiModelProperty(
    dataType = "String",
    readOnly = true,
    value = "The canonical identifier given to a thing.") id: Option[String],
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.v1.DisplayIdentifierV1]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifierV1]],
  @ApiModelProperty(
    value = "The name of the organisation"
  ) label: String,
  @JsonProperty("type") ontologyType: String = "Organisation")
    extends DisplayAbstractAgentV1
