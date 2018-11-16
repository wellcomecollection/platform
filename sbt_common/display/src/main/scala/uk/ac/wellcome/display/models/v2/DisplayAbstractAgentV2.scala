package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal._

@ApiModel(
  value = "Agent"
)
sealed trait DisplayAbstractAgentV2 extends DisplayAbstractRootConcept

@ApiModel(
  value = "Agent"
)
case class DisplayAgentV2(
  id: Option[String],
  identifiers: Option[List[DisplayIdentifierV2]],
  @ApiModelProperty(
    value = "The name of the agent"
  ) label: String,
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "Agent"
) extends DisplayAbstractAgentV2

case object DisplayAbstractAgentV2 {
  def apply(displayableAgent: Displayable[AbstractAgent],
            includesIdentifiers: Boolean): DisplayAbstractAgentV2 =
    displayableAgent match {
      case Unidentifiable(a: Agent) =>
        DisplayAgentV2(
          id = None,
          identifiers = None,
          label = a.label
        )
      case Identified(
          agent: Agent,
          canonicalId,
          sourceIdentifier,
          otherIdentifiers) =>
        DisplayAgentV2(
          id = Some(canonicalId),
          identifiers =
            if (includesIdentifiers)
              Some(
                (sourceIdentifier +: otherIdentifiers).map(
                  DisplayIdentifierV2(_)))
            else None,
          label = agent.label
        )
      case Identified(
          person: Person,
          canonicalId,
          sourceIdentifier,
          otherIdentifiers) =>
        DisplayPersonV2(
          id = Some(canonicalId),
          identifiers =
            if (includesIdentifiers)
              Some(
                (sourceIdentifier +: otherIdentifiers).map(
                  DisplayIdentifierV2(_)))
            else None,
          label = person.label,
          prefix = person.prefix,
          numeration = person.numeration
        )
      case Unidentifiable(p: Person) =>
        DisplayPersonV2(
          id = None,
          identifiers = None,
          label = p.label,
          prefix = p.prefix,
          numeration = p.numeration)
      case Identified(
          org: Organisation,
          canonicalId,
          sourceIdentifier,
          otherIdentifiers) =>
        DisplayOrganisationV2(
          id = Some(canonicalId),
          identifiers =
            if (includesIdentifiers)
              Some(
                (sourceIdentifier +: otherIdentifiers).map(
                  DisplayIdentifierV2(_)))
            else None,
          label = org.label
        )
      case Unidentifiable(o: Organisation) =>
        DisplayOrganisationV2(id = None, identifiers = None, label = o.label)
    }
}

@ApiModel(
  value = "Person"
)
case class DisplayPersonV2(
  @ApiModelProperty(
    dataType = "String",
    readOnly = true,
    value = "The canonical identifier given to a thing.") id: Option[String],
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.v2.DisplayIdentifierV2]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifierV2]],
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
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "Person")
    extends DisplayAbstractAgentV2

@ApiModel(
  value = "Organisation"
)
case class DisplayOrganisationV2(
  @ApiModelProperty(
    dataType = "String",
    readOnly = true,
    value = "The canonical identifier given to a thing.") id: Option[String],
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.v2.DisplayIdentifierV2]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifierV2]],
  @ApiModelProperty(
    value = "The name of the organisation"
  ) label: String,
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "Organisation")
    extends DisplayAbstractAgentV2
