package uk.ac.wellcome.models.work.generators

import uk.ac.wellcome.models.generators.RandomStrings
import uk.ac.wellcome.models.work.internal._

trait ProductionEventGenerators extends RandomStrings {
  def createProductionEventWith(function: Option[Concept] = None)
    : ProductionEvent[Displayable[AbstractAgent]] =
    ProductionEvent(
      label = randomAlphanumeric(25),
      places = List(Place(randomAlphanumeric(10))),
      agents = List(Unidentifiable(Person(randomAlphanumeric(10)))),
      dates = List(Period(randomAlphanumeric(5))),
      function = function
    )

  def createProductionEvent: ProductionEvent[Displayable[AbstractAgent]] =
    createProductionEventWith()

  def createProductionEventList(
    count: Int = 1): List[ProductionEvent[Displayable[AbstractAgent]]] =
    (1 to count).map { _ =>
      createProductionEvent
    }.toList
}
