package uk.ac.wellcome.models.work.internal

case class Item(
  locations: List[Location] = List(),
  ontologyType: String = "Item"
)
