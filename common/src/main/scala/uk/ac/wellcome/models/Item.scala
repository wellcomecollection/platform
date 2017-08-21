package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

case class Item(
  canonicalId: Option[String],
  identifiers: List[SourceIdentifier],
  locations: List[Location]
) {
  @JsonProperty("type") val ontologyType: String = "Item"
}
