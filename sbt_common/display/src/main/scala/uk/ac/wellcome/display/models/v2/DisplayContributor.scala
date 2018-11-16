package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.{
  AbstractAgent,
  Contributor,
  Displayable
}

@ApiModel(
  value = "Contributor",
  description = "A contributor"
)
case class DisplayContributor(
  @ApiModelProperty(value = "The agent.") agent: DisplayAbstractAgentV2,
  @ApiModelProperty(value = "The list of contribution roles.") roles: List[
    DisplayContributionRole],
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "Contributor"
)

object DisplayContributor {
  def apply(contributor: Contributor[Displayable[AbstractAgent]],
            includesIdentifiers: Boolean): DisplayContributor =
    DisplayContributor(
      agent = DisplayAbstractAgentV2(
        contributor.agent,
        includesIdentifiers = includesIdentifiers),
      roles = contributor.roles.map { DisplayContributionRole(_) }
    )
}
