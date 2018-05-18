package uk.ac.wellcome.platform.matcher.workgraph

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.models.{LinkedWork, LinkedWorksGraph}

class LinkedWorkGraphUpdaterTest extends FunSpec with Matchers {

  it("an update A given nothing creates A") {
   LinkedWorkGraphUpdater.update(
     workUpdate = LinkedWork("A"),
     existingGraph = LinkedWorksGraph(List())
   ).linkedWorksList should contain theSameElementsAs
     List(LinkedWork("A"))
  }

  it("an update A->B given nothing creates A->B") {
    LinkedWorkGraphUpdater.update(
      workUpdate = LinkedWork("A", List("B")),
      existingGraph = LinkedWorksGraph(List())
    ).linkedWorksList should contain theSameElementsAs
      List(LinkedWork("A", List("B")))
  }

  it("an update B->A given nothing creates B->A") {
    LinkedWorkGraphUpdater.update(
      workUpdate = LinkedWork("B", List("A")),
      existingGraph = LinkedWorksGraph(List())
    ).linkedWorksList should contain theSameElementsAs
      List(LinkedWork("B", List("A")))
  }

  it("an update A->B given A->B leaves A->B") {
    LinkedWorkGraphUpdater.update(
      workUpdate = LinkedWork("A", List("B")),
      existingGraph = LinkedWorksGraph(List(LinkedWork("A", List("B"))))
    ).linkedWorksList should contain theSameElementsAs
      List(LinkedWork("A", List("B")))
  }

  it("an update B->C given A->B creates A->B->C") {
    LinkedWorkGraphUpdater.update(
      workUpdate = LinkedWork("B", List("C")),
      existingGraph = LinkedWorksGraph(List(LinkedWork("A", List("B"))))
    ).linkedWorksList should contain theSameElementsAs
      List(LinkedWork("A", List("B")), LinkedWork("B", List("C")))
  }
}
