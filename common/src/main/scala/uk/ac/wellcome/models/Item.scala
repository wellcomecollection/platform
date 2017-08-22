package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

case class Item(
  canonicalId: Option[String],
  identifiers: List[SourceIdentifier],
  locations: List[Location]
) extends Identifiable {
  @JsonProperty("type") val ontologyType: String = "Item"
}

case object Item {
  def apply(
    canonicalId: String,
    identifiers: List[SourceIdentifier],
    locations: List[Location]
  ): Item =
    Item(Some(canonicalId), identifiers, locations)
}
