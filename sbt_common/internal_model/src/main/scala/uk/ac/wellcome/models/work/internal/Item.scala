package uk.ac.wellcome.models.work.internal

sealed trait Item extends MultiplyIdentified {
  val sourceIdentifier: SourceIdentifier
  val otherIdentifiers: List[SourceIdentifier]
  val locations: List[Location]
  val ontologyType: String
}

case class UnidentifiedItem(
  sourceIdentifier: SourceIdentifier,
  otherIdentifiers: List[SourceIdentifier] = Nil,
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
