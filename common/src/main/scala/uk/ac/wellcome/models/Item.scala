package uk.ac.wellcome.models

sealed trait Item extends Identifiable {
  val sourceIdentifier: SourceIdentifier
  val identifiers: List[SourceIdentifier]
  val locations: List[Location]
  val visible: Boolean
  val ontologyType: String
}

case class UnidentifiedItem(
  sourceIdentifier: SourceIdentifier,
  identifiers: List[SourceIdentifier] = Nil,
  locations: List[Location] = List(),
  visible: Boolean = true,
  ontologyType: String = "Item"
) extends Identifiable
    with Item

case class IdentifiedItem(
  canonicalId: String,
  sourceIdentifier: SourceIdentifier,
  identifiers: List[SourceIdentifier] = Nil,
  locations: List[Location] = List(),
  visible: Boolean = true,
  ontologyType: String = "Item"
) extends Identifiable
    with Item
