package uk.ac.wellcome.platform.matcher.workgraph

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.models.{LinkedWork, LinkedWorkUpdate, LinkedWorksGraph}

class LinkedWorkGraphUpdaterTest extends FunSpec with Matchers {

  // An existing graph of works is updated by changing the links of a single work.
  // The change may result in new compound works which are identified by the LinkedWorkGraphUpdater
  // works are shown by id with directed links, A->B means work with id "A" is linked to work with id "B"
  // works comprised of linked works are identified by compound id together with their linked works.
  // A+B:(A->B, B) means compound work with id "A+B" is made of work A linked to B and work B.

  describe("Adding links without existing works") {
    it("updating nothing with A gives A:A") {
      LinkedWorkGraphUpdater
        .update(
          workUpdate = LinkedWorkUpdate("A", Set.empty),
          existingGraph = LinkedWorksGraph(Set.empty)
        )
        .linkedWorksSet shouldBe Set(LinkedWork("A", List(), setId = "A"))
    }

    it("updating nothing with A->B gives A+B:A->B") {
      LinkedWorkGraphUpdater
        .update(
          workUpdate = LinkedWorkUpdate("A", Set("B")),
          existingGraph = LinkedWorksGraph(Set.empty)
        )
        .linkedWorksSet shouldBe Set(
          LinkedWork("A", List("B"), setId = "A+B"),
          LinkedWork("B", List(), setId = "A+B"))
    }

    it("updating nothing with B->A gives A+B:B->A") {
      LinkedWorkGraphUpdater
        .update(
          workUpdate = LinkedWorkUpdate("B", Set("A")),
          existingGraph = LinkedWorksGraph(Set.empty)
        )
        .linkedWorksSet shouldBe Set(
          LinkedWork("B", List("A"), setId = "A+B"),
          LinkedWork("A", List(), setId = "A+B"))
    }
  }

  describe("Adding links to existing works") {
    it("updating A->B with A->B gives A+B:(A->B, B)") {
      LinkedWorkGraphUpdater
        .update(
          workUpdate = LinkedWorkUpdate("A",Set("B")),
          existingGraph = LinkedWorksGraph(Set(LinkedWork("A", List("B"), "A+B")))
        )
        .linkedWorksSet should contain theSameElementsAs
        List(
          LinkedWork("A", List("B"), setId = "A+B"),
          LinkedWork("B", List(), setId = "A+B"))
    }

    it("updating A->B with B->C gives A+B+C:(A->B, B->C, C)") {
      LinkedWorkGraphUpdater
        .update(
          workUpdate = LinkedWorkUpdate("B", Set("C")),
          existingGraph = LinkedWorksGraph(Set(LinkedWork("A", List("B"), "A+B")))
        )
        .linkedWorksSet shouldBe Set(
          LinkedWork("A", List("B"), setId = "A+B+C"),
          LinkedWork("B", List("C"), setId = "A+B+C"),
          LinkedWork("C", List(), setId = "A+B+C")
        )
    }

    it("updating A->B, C->D with B->C gives A+B+C+D:(A->B, B->C, C->D, D)") {
      LinkedWorkGraphUpdater
        .update(
          workUpdate = LinkedWorkUpdate("B", Set("C")),
          existingGraph = LinkedWorksGraph(
            Set(LinkedWork("A", List("B"), "A+B"), LinkedWork("C", List("D"), "C+D")))
        )
        .linkedWorksSet should contain theSameElementsAs
        List(
          LinkedWork("A", List("B"), "A+B+C+D"),
          LinkedWork("B", List("C"), "A+B+C+D"),
          LinkedWork("C", List("D"), "A+B+C+D"),
          LinkedWork("D", List(), setId = "A+B+C+D"))
    }

    it("updating A->B with B->[C,D] gives A+B+C+D:(A->B, B->C&D, C, D") {
      LinkedWorkGraphUpdater
        .update(
          workUpdate = LinkedWorkUpdate("B", Set("C", "D")),
          existingGraph = LinkedWorksGraph(Set(LinkedWork("A", List("B"), "A+B")))
        )
        .linkedWorksSet should contain theSameElementsAs
        List(
          LinkedWork("A", List("B"), setId = "A+B+C+D"),
          LinkedWork("B", List("C", "D"), setId = "A+B+C+D"),
          LinkedWork("C", List(), setId = "A+B+C+D"),
          LinkedWork("D", List(), setId = "A+B+C+D")
        )
    }

    it("updating A->B->C with A->C gives A+B+C:(A->B, B->C, C->A") {
      LinkedWorkGraphUpdater
        .update(
          workUpdate = LinkedWorkUpdate("C", Set("A")),
          existingGraph = LinkedWorksGraph(
            Set(LinkedWork("A", List("B"), "A+B"), LinkedWork("B", List("C"), "B+C")))
        )
        .linkedWorksSet shouldBe Set(
          LinkedWork("A", List("B"), setId = "A+B+C"),
          LinkedWork("B", List("C"), setId = "A+B+C"),
          LinkedWork("C", List("A"), setId = "A+B+C")
        )
    }
  }

  describe("Removing links") {
    it("updating  A->B, B with A gives A:A and B:B") {
      LinkedWorkGraphUpdater
        .update(
          workUpdate = LinkedWorkUpdate("A", Set.empty),
          existingGraph =
            LinkedWorksGraph(Set(LinkedWork("A", List("B"), "A+B"), LinkedWork("B", List(), "A+B")))
        )
        .linkedWorksSet shouldBe Set(
          LinkedWork("A", List(), setId = "A"), LinkedWork("B", List(), setId = "B"))
    }

    it(
      "updating A->B with A but NO B (*should* not be possible) gives A:A and B:B") {
      LinkedWorkGraphUpdater
        .update(
          workUpdate = LinkedWorkUpdate("A", Set.empty),
          existingGraph = LinkedWorksGraph(Set(LinkedWork("A", List("B"), "A+B")))
        )
        .linkedWorksSet shouldBe Set(
          LinkedWork("A", List(), setId = "A"), LinkedWork("B", List(), setId = "B"))
    }

    it("updating A->B->C with B gives A+B:(A->B, B) and C:C") {
      LinkedWorkGraphUpdater
        .update(
          workUpdate = LinkedWorkUpdate("B", Set.empty),
          existingGraph = LinkedWorksGraph(
            Set(LinkedWork("A", List("B"), "A+B+C"), LinkedWork("B", List("C"), "A+B+C")))
        )
        .linkedWorksSet shouldBe Set(
          LinkedWork("A", List("B"), setId = "A+B"),
          LinkedWork("B", List(), setId = "A+B"),
          LinkedWork("C", List(), setId = "C"))
    }

    it("updating A<->B->C with B->C gives A+B+C:(A->B, B->C, C)") {
      LinkedWorkGraphUpdater
        .update(
          workUpdate = LinkedWorkUpdate("B", Set("C")),
          existingGraph = LinkedWorksGraph(
            Set(
              LinkedWork("A", List("B"), "A+B+C"),
              LinkedWork("B", List("A", "C"), "A+B+C"),
              LinkedWork("C", List(), "A+B+C")))
        )
        .linkedWorksSet shouldBe Set(
          LinkedWork("A", List("B"), setId = "A+B+C"),
          LinkedWork("B", List("C"), setId = "A+B+C"),
          LinkedWork("C", List(), setId = "A+B+C"))
    }
  }
}
