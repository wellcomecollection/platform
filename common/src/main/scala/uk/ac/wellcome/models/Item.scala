package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

case class Item(
  identifiers: List[SourceIdentifier],
  locations: List[Location]
) {
  @JsonProperty("type") val ontologyType: String = "Item"
}
