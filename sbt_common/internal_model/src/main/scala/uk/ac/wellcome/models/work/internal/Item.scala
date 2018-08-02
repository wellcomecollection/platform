package uk.ac.wellcome.models.work.internal

case class Item[+T <: IdentityState[String] ](
  locations: List[Location],
  v1SourceIdentifier: T,
  ontologyType: String = "Item"
)
