package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty

case class Item(
  canonicalId: Option[String] = None,
  sourceIdentifier: SourceIdentifier,
  identifiers: List[SourceIdentifier] = Nil,
  locations: List[Location] = List(),
  visible: Boolean = true,
  @JsonProperty("type") ontologyType: String = "Item"
) extends Identifiable
