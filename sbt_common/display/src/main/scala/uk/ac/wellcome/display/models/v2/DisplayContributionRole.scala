package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.ContributionRole

@ApiModel(
  value = "ContributionRole",
  description = "A contribution role"
)
case class DisplayContributionRole(
  @ApiModelProperty(
    value = "The name of the agent"
  ) label: String,
  @JsonProperty("type") ontologyType: String = "ContributionRole"
)

object DisplayContributionRole {
  def apply(contributionRole: ContributionRole): DisplayContributionRole =
    DisplayContributionRole(
      label = contributionRole.label
    )
}
