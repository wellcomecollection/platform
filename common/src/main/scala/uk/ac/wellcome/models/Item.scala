package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

case class Item(
  canonicalId: Option[String] = None,
  identifiers: List[SourceIdentifier],
  locations: List[Location]
) extends Identifiable {
  @JsonProperty("type") val ontologyType: String = "Item"
}
