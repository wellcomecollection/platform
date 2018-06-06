package uk.ac.wellcome.models.work.internal

case class ProductionEvent[+T <: IdentityState[AbstractAgent]](
  places: List[Place],
  productionFunction: Option[Concept],
  dates: List[Period],
  agents: List[T],
  ontologyType: String = "ProductionEvent"
)
