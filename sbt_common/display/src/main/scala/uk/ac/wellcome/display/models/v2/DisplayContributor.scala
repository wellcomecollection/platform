package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import uk.ac.wellcome.display.models.DisplayAbstractAgent

case class DisplayContributor (
                              agent: DisplayAbstractAgent,
                              roles: List[DisplayContributionRole],
  @JsonProperty("type") ontologyType: String = "Contributor"
                              )
