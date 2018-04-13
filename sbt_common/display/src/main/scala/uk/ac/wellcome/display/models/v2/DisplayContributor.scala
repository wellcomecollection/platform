package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.display.models.DisplayAbstractAgent

@ApiModel(
  value = "Contributor",
  description =
    "A contributor"
)
case class DisplayContributor (
                                @ApiModelProperty(
                                  dataType = "uk.ac.wellcome.display.models.DisplayAbstractAgent",
                                  value = "The agent.")
                                agent: DisplayAbstractAgent,
                                @ApiModelProperty(
                                  dataType = "List[uk.ac.wellcome.display.models.v2.DisplayContributionRole]",
                                  value = "The agent.")
                              roles: List[DisplayContributionRole],
  @JsonProperty("type") ontologyType: String = "Contributor"
                              )
