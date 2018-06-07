package uk.ac.wellcome.models.work.internal

case class ProductionEvent[+T <: IdentityState[AbstractAgent]](
  places: List[Place],
  agents: List[T],
  dates: List[Period],
  productionFunction: Option[Concept],
  ontologyType: String = "ProductionEvent"
)
