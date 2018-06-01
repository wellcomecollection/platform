package uk.ac.wellcome.platform.matcher.workgraph

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.matcher.WorkNode
import uk.ac.wellcome.platform.matcher.models.{WorkGraph, WorkUpdate}

class WorkGraphUpdaterTest extends FunSpec with Matchers {

  // An existing graph of works is updated by changing the links of a single work.
  // The change may result in new compound works which are identified by the LinkedWorkGraphUpdater
  // works are shown by id with directed links, A->B means work with id "A" is linked to work with id "B"
  // works comprised of linked works are identified by compound id together with their linked works.
  // A+B:(A->B, B) means compound work with id "A+B" is made of work A linked to B and work B.

  describe("Adding links without existing works") {
    it("updating nothing with A gives A:A") {
      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("A", 1, Set.empty),
          existingGraph = WorkGraph(Set.empty)
        )
        .nodes shouldBe Set(WorkNode("A", 1, List(), componentId = "A"))
    }

    it("updating nothing with A->B gives A+B:A->B") {
      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("A", 1, Set("B")),
          existingGraph = WorkGraph(Set.empty)
        )
        .nodes shouldBe Set(
        WorkNode("A", 1, List("B"), componentId = "A+B"),
        WorkNode("B", 0, List(), componentId = "A+B"))
    }

    it("updating nothing with B->A gives A+B:B->A") {
      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("B", 1, Set("A")),
          existingGraph = WorkGraph(Set.empty)
        )
        .nodes shouldBe Set(
        WorkNode("B", 1, List("A"), componentId = "A+B"),
        WorkNode("A", 0, List(), componentId = "A+B"))
    }
  }

  describe("Adding links to existing works") {
    it("updating A, B with A->B gives A+B:(A->B, B)") {
      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("A", 2, Set("B")),
          existingGraph = WorkGraph(
            Set(WorkNode("A", 1, Nil, "A"), WorkNode("B", 1, Nil, "B")))
        )
        .nodes should contain theSameElementsAs
        List(
          WorkNode("A", 2, List("B"), componentId = "A+B"),
          WorkNode("B", 1, List(), componentId = "A+B"))
    }

    it("updating A->B with A->B gives A+B:(A->B, B)") {
      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("A", 2, Set("B")),
          existingGraph = WorkGraph(
            Set(
              WorkNode("A", 1, List("B"), "A+B"),
              WorkNode("B", 1, Nil, "A+B")))
        )
        .nodes should contain theSameElementsAs
        List(
          WorkNode("A", 2, List("B"), componentId = "A+B"),
          WorkNode("B", 1, List(), componentId = "A+B"))
    }

    it("updating A->B, B, C with B->C gives A+B+C:(A->B, B->C, C)") {
      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("B", 2, Set("C")),
          existingGraph = WorkGraph(
            Set(
              WorkNode("A", 2, List("B"), "A+B"),
              WorkNode("B", 1, Nil, "A+B"),
              WorkNode("C", 1, Nil, "C")))
        )
        .nodes shouldBe Set(
        WorkNode("A", 2, List("B"), componentId = "A+B+C"),
        WorkNode("B", 2, List("C"), componentId = "A+B+C"),
        WorkNode("C", 1, List(), componentId = "A+B+C")
      )
    }

    it("updating A->B, C->D with B->C gives A+B+C+D:(A->B, B->C, C->D, D)") {
      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("B", 2, Set("C")),
          existingGraph = WorkGraph(
            Set(
              WorkNode("A", 1, List("B"), "A+B"),
              WorkNode("C", 1, List("D"), "C+D"),
              WorkNode("B", 1, Nil, "A+B"),
              WorkNode("D", 1, Nil, "C+D")
            ))
        )
        .nodes should contain theSameElementsAs
        List(
          WorkNode("A", 1, List("B"), "A+B+C+D"),
          WorkNode("B", 2, List("C"), "A+B+C+D"),
          WorkNode("C", 1, List("D"), "A+B+C+D"),
          WorkNode("D", 1, List(), "A+B+C+D"))
    }

    it("updating A->B with B->[C,D] gives A+B+C+D:(A->B, B->C&D, C, D") {
      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("B", 2, Set("C", "D")),
          existingGraph = WorkGraph(
            Set(
              WorkNode("A", 2, List("B"), "A+B"),
              WorkNode("B", 1, Nil, "A+B"),
              WorkNode("C", 1, Nil, "C"),
              WorkNode("D", 1, Nil, "D")
            ))
        )
        .nodes should contain theSameElementsAs
        List(
          WorkNode("A", 2, List("B"), componentId = "A+B+C+D"),
          WorkNode("B", 2, List("C", "D"), componentId = "A+B+C+D"),
          WorkNode("C", 1, List(), componentId = "A+B+C+D"),
          WorkNode("D", 1, List(), componentId = "A+B+C+D")
        )
    }

    it("updating A->B->C with A->C gives A+B+C:(A->B, B->C, C->A") {
      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("C", 2, Set("A")),
          existingGraph = WorkGraph(
            Set(
              WorkNode("A", 2, List("B"), "A+B+C"),
              WorkNode("B", 2, List("C"), "A+B+C"),
              WorkNode("C", 1, Nil, "A+B+C")))
        )
        .nodes shouldBe Set(
        WorkNode("A", 2, List("B"), componentId = "A+B+C"),
        WorkNode("B", 2, List("C"), componentId = "A+B+C"),
        WorkNode("C", 2, List("A"), componentId = "A+B+C")
      )
    }
  }

  describe("Removing links") {
    it("updating  A->B with A gives A:A and B:B") {
      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("A", 2, Set.empty),
          existingGraph = WorkGraph(
            Set(
              WorkNode("A", 1, List("B"), "A+B"),
              WorkNode("B", 1, List(), "A+B")))
        )
        .nodes shouldBe Set(
        WorkNode("A", 2, List(), componentId = "A"),
        WorkNode("B", 1, List(), componentId = "B")
      )
    }

    it("updating A->B with A but NO B (*should* not occur) gives A:A and B:B") {
      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("A", 2, Set.empty),
          existingGraph = WorkGraph(
            Set(
              WorkNode("A", 1, List("B"), "A+B")
            ))
        )
        .nodes shouldBe Set(
        WorkNode("A", 2, Nil, componentId = "A"),
        WorkNode("B", 0, Nil, componentId = "B")
      )
    }

    it("updating A->B->C with B gives A+B:(A->B, B) and C:C") {
      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("B", 3, Set.empty),
          existingGraph = WorkGraph(
            Set(
              WorkNode("A", 2, List("B"), "A+B+C"),
              WorkNode("B", 2, List("C"), "A+B+C"),
              WorkNode("C", 1, Nil, "A+B+C")))
        )
        .nodes shouldBe Set(
        WorkNode("A", 2, List("B"), "A+B"),
        WorkNode("B", 3, Nil, "A+B"),
        WorkNode("C", 1, Nil, "C")
      )
    }

    it("updating A<->B->C with B->C gives A+B+C:(A->B, B->C, C)") {
      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("B", 3, Set("C")),
          existingGraph = WorkGraph(
            Set(
              WorkNode("A", 2, List("B"), "A+B+C"),
              WorkNode("B", 2, List("A", "C"), "A+B+C"),
              WorkNode("C", 1, Nil, "A+B+C")))
        )
        .nodes shouldBe Set(
        WorkNode("A", 2, List("B"), "A+B+C"),
        WorkNode("B", 3, List("C"), "A+B+C"),
        WorkNode("C", 1, Nil, "A+B+C")
      )
    }
  }
}
