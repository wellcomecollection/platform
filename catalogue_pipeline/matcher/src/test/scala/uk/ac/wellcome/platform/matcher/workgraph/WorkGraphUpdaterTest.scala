package uk.ac.wellcome.platform.matcher.workgraph

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.matcher.WorkNode
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.models.{WorkGraph, WorkUpdate}

class WorkGraphUpdaterTest extends FunSpec with Matchers with MatcherFixtures {

  // An existing graph of works is updated by changing the links of a single work.
  // The change may result in new compound works which are identified by the LinkedWorkGraphUpdater
  // works are shown by id with directed links, A->B means work with id "A" is linked to work with id "B"
  // works comprised of linked works are identified by compound id together with their linked works.
  // A+B:(A->B, B) means compound work with id "A+B" is made of work A linked to B and work B.

  private val hashed_A    = "217b21f3" // ciHash("A")
  private val hashed_B    = "4d7e95ff" // ciHash("B")
  private val hashed_C    = "8171cb29" // ciHash("C")
  private val hashed_AB   = "2f2f1575" // ciHash("A+B")
  private val hashed_ABC  = "45e72cb2" // ciHash("A+B+C")
  private val hashed_ABCD = "73200083" // ciHash("A+B+C+D")

  describe("Adding links without existing works") {
    it("updating nothing with A gives A:A") {
      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("A", 1, Set.empty),
          existingGraph = WorkGraph(Set.empty)
        )
        .nodes shouldBe Set(WorkNode("A", 1, List(), hashed_A))
    }

    it("updating nothing with A->B gives A+B:A->B") {
      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("A", 1, Set("B")),
          existingGraph = WorkGraph(Set.empty)
        )
        .nodes shouldBe Set(
        WorkNode("A", 1, List("B"), hashed_AB),
        WorkNode("B", 0, List(), hashed_AB))
    }

    it("updating nothing with B->A gives A+B:B->A") {
      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("B", 1, Set("A")),
          existingGraph = WorkGraph(Set.empty)
        )
        .nodes shouldBe Set(
        WorkNode("B", 1, List("A"), hashed_AB),
        WorkNode("A", 0, List(), hashed_AB))
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
          WorkNode("A", 2, List("B"), hashed_AB),
          WorkNode("B", 1, List(), hashed_AB))
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
        .nodes shouldBe Set(
          WorkNode("A", 2, List("B"), hashed_AB),
          WorkNode("B", 1, List(), hashed_AB)
        )
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
        WorkNode("A", 2, List("B"), hashed_ABC),
        WorkNode("B", 2, List("C"), hashed_ABC),
        WorkNode("C", 1, List(), hashed_ABC)
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
        .nodes shouldBe
        Set(
          WorkNode("A", 1, List("B"), hashed_ABCD),
          WorkNode("B", 2, List("C"), hashed_ABCD),
          WorkNode("C", 1, List("D"), hashed_ABCD),
          WorkNode("D", 1, List(), hashed_ABCD))
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
        .nodes shouldBe
        Set(
          WorkNode("A", 2, List("B"), hashed_ABCD),
          WorkNode("B", 2, List("C", "D"),hashed_ABCD),
          WorkNode("C", 1, List(), hashed_ABCD),
          WorkNode("D", 1, List(), hashed_ABCD)
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
        WorkNode("A", 2, List("B"), hashed_ABC),
        WorkNode("B", 2, List("C"), hashed_ABC),
        WorkNode("C", 2, List("A"), hashed_ABC)
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
        WorkNode("A", 2, List(), hashed_A),
        WorkNode("B", 1, List(), hashed_B)
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
        WorkNode("A", 2, Nil, hashed_A),
        WorkNode("B", 0, Nil, hashed_B)
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
        WorkNode("A", 2, List("B"),hashed_AB),
        WorkNode("B", 3, Nil, hashed_AB),
        WorkNode("C", 1, Nil, hashed_C)
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
        WorkNode("A", 2, List("B"),hashed_ABC),
        WorkNode("B", 3, List("C"), hashed_ABC),
        WorkNode("C", 1, Nil, hashed_ABC)
      )
    }
  }
}
