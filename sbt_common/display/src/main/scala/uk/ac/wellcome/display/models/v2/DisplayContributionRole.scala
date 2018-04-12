package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import uk.ac.wellcome.display.models.DisplayAbstractAgent

case class DisplayContributionRole (
                              label: String,
  @JsonProperty("type") ontologyType: String = "ContributionRole"
                              )
