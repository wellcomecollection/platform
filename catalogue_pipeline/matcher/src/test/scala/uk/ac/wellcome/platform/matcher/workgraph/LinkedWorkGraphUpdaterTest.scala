package uk.ac.wellcome.platform.matcher.workgraph

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.models.{LinkedWork, LinkedWorksGraph}

class LinkedWorkGraphUpdaterTest extends FunSpec with Matchers {

  it("an update A given nothing creates A:A") {
   LinkedWorkGraphUpdater.update(
     workUpdate = LinkedWork("A"),
     existingGraph = LinkedWorksGraph(List())
   ).linkedWorksList should contain theSameElementsAs
     List(LinkedWork("A", setId = "A"))
  }

  it("an update A->B given nothing creates A->B") {
    LinkedWorkGraphUpdater.update(
      workUpdate = LinkedWork("A", List("B")),
      existingGraph = LinkedWorksGraph(List())
    ).linkedWorksList should contain theSameElementsAs
      List(
        LinkedWork("A", List("B"), setId = "A+B"),
        LinkedWork("B", setId = "A+B"))
  }

  it("an update B->A given nothing creates B->A") {
    LinkedWorkGraphUpdater.update(
      workUpdate = LinkedWork("B", List("A")),
      existingGraph = LinkedWorksGraph(List())
    ).linkedWorksList should contain theSameElementsAs
      List(
        LinkedWork("B", List("A"), setId = "A+B"),
        LinkedWork("A", setId = "A+B"))
  }

  it("an update A->B given A->B leaves A->B") {
    LinkedWorkGraphUpdater.update(
      workUpdate = LinkedWork("A", List("B")),
      existingGraph = LinkedWorksGraph(List(LinkedWork("A", List("B"))))
    ).linkedWorksList should contain theSameElementsAs
      List(
        LinkedWork("A", List("B"), setId = "A+B"),
        LinkedWork("B", setId = "A+B"))
  }

  it("an update B->C given A->B creates A->B->C") {
    LinkedWorkGraphUpdater.update(
      workUpdate = LinkedWork("B", List("C")),
      existingGraph = LinkedWorksGraph(List(LinkedWork("A", List("B"))))
    ).linkedWorksList should contain theSameElementsAs
      List(
        LinkedWork("A", List("B"), setId = "A+B+C"),
        LinkedWork("B", List("C"), setId = "A+B+C"),
        LinkedWork("C", setId = "A+B+C")
      )
  }

  it("an update B->C given A->B, C->D creates A->B->C->D") {
    LinkedWorkGraphUpdater.update(
      workUpdate = LinkedWork("B", List("C")),
      existingGraph = LinkedWorksGraph(List(LinkedWork("A", List("B")),LinkedWork("C", List("D"))))
    ).linkedWorksList should contain theSameElementsAs
      List(
        LinkedWork("A", List("B"), "A+B+C+D"),
        LinkedWork("B", List("C"), "A+B+C+D"),
        LinkedWork("C", List("D"), "A+B+C+D"),
        LinkedWork("D", setId = "A+B+C+D"))
  }

  it("an update B->C&D given A->B creates A->B->C&D") {
    LinkedWorkGraphUpdater.update(
      workUpdate = LinkedWork("B", List("C", "D")),
      existingGraph = LinkedWorksGraph(List(LinkedWork("A", List("B"))))
    ).linkedWorksList should contain theSameElementsAs
      List(
        LinkedWork("A", List("B"), setId = "A+B+C+D"),
        LinkedWork("B", List("C","D"), setId = "A+B+C+D"),
        LinkedWork("C", setId = "A+B+C+D"),
        LinkedWork("D", setId = "A+B+C+D")
      )
  }

  it("an update A->C given A->B->C creates A->B->C->A") {
    LinkedWorkGraphUpdater.update(
      workUpdate = LinkedWork("C", List("A")),
      existingGraph = LinkedWorksGraph(List(
          LinkedWork("A", List("B")),
          LinkedWork("B", List("C"))))
    ).linkedWorksList should contain theSameElementsAs
      List(
        LinkedWork("A", List("B"), setId = "A+B+C"),
        LinkedWork("B", List("C"), setId = "A+B+C"),
        LinkedWork("C", List("A"), setId = "A+B+C")
      )
  }

  describe("Updates removing nodes") {
    it("an update A->B given A and B leaves A and B") {
      LinkedWorkGraphUpdater.update(
        workUpdate = LinkedWork("A"),
        existingGraph = LinkedWorksGraph(List(
          LinkedWork("A", List("B")),
          LinkedWork("B")))
      ).linkedWorksList should contain theSameElementsAs
        List(
          LinkedWork("A", setId = "A"),
          LinkedWork("B", setId = "B"))
    }

    it("an update A->B given A but NOT B (*should* not be possible) leaves A and B") {
      LinkedWorkGraphUpdater.update(
        workUpdate = LinkedWork("A"),
        existingGraph = LinkedWorksGraph(List(
          LinkedWork("A", List("B"))))
      ).linkedWorksList should contain theSameElementsAs
        List(
          LinkedWork("A", setId = "A"),
          LinkedWork("B", setId = "B"))
    }
  }
}
