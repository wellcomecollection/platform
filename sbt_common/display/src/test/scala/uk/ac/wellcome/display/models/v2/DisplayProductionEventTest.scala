package uk.ac.wellcome.display.models.v2

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.ProductionEventGenerators
import uk.ac.wellcome.models.work.internal._

class DisplayProductionEventTest
    extends FunSpec
    with Matchers
    with ProductionEventGenerators {
  it("serialises a DisplayProductionEvent from a ProductionEvent") {
    val productionEvent = ProductionEvent(
      label = "London, Macmillan, 2005",
      places = List(Place("London")),
      agents = List(Unidentifiable(Agent("Macmillan"))),
      dates = List(Period("2005")),
      function = Some(Concept("Manufacture"))
    )

    val displayProductionEvent = DisplayProductionEvent(
      productionEvent,
      includesIdentifiers = false
    )
    displayProductionEvent shouldBe DisplayProductionEvent(
      label = "London, Macmillan, 2005",
      places = List(DisplayPlace(label = "London")),
      agents = List(
        DisplayAgentV2(
          id = None,
          identifiers = None,
          label = "Macmillan"
        )),
      dates = List(DisplayPeriod(label = "2005")),
      function = Some(DisplayConcept(label = "Manufacture"))
    )
  }

  it("serialises a DisplayProductionEvent without a function") {
    val productionEvent = createProductionEventWith(
      function = None
    )

    val displayProductionEvent = DisplayProductionEvent(
      productionEvent,
      includesIdentifiers = false
    )
    displayProductionEvent.function shouldBe None
  }
}
