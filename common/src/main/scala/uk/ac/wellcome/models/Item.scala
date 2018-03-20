package uk.ac.wellcome.models

sealed trait Item {
  val sourceIdentifier: SourceIdentifier
  val identifiers: List[SourceIdentifier]
  val locations: List[Location]
  val ontologyType: String
}

case class UnidentifiedItem(
  sourceIdentifier: SourceIdentifier,
  identifiers: List[SourceIdentifier] = Nil,
  locations: List[Location] = List(),
  ontologyType: String = "Item"
) extends Item

case class IdentifiedItem(
  canonicalId: String,
  sourceIdentifier: SourceIdentifier,
  identifiers: List[SourceIdentifier] = Nil,
  locations: List[Location] = List(),
  ontologyType: String = "Item"
) extends Item
