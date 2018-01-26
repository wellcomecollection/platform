package uk.ac.wellcome.models

case class Item(
  canonicalId: Option[String] = None,
  sourceIdentifier: SourceIdentifier,
  identifiers: List[SourceIdentifier] = Nil,
  locations: List[Location] = List(),
  visible: Boolean = true,
  ontologyType: String = "Item"
) extends Identifiable
