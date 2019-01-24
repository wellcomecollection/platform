package uk.ac.wellcome.platform.matcher.workgraph

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.matcher.WorkNode
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.models._

class WorkGraphUpdaterTest extends FunSpec with Matchers with MatcherFixtures {

  // An existing graph of works is updated by changing the links of a single work.
  // The change may result in new compound works which are identified by the LinkedWorkGraphUpdater
  // works are shown by id with directed links, A->B means work with id "A" is linked to work with id "B"
  // works comprised of linked works are identified by compound id together with their linked works.
  // A+B:(A->B, B) means compound work with id "A+B" is made of work A linked to B and work B.

  private val hashed_A =
    "559aead08264d5795d3909718cdd05abd49572e84fe55590eef31a88a08fdffd" // ciHash("A")
  private val hashed_B =
    "df7e70e5021544f4834bbee64a9e3789febc4be81470df629cad6ddb03320a5c" // ciHash("B")
  private val hashed_C =
    "6b23c0d5f35d1b11f9b683f0b0a617355deb11277d91ae091d399c655b87940d" // ciHash("C")
  private val hashed_AB =
    "ca5b7e1c5b0ddba53ac5a73e3c49e9bb896a6f488ce4d605d24a6debedcc901d" // ciHash("A+B")
  private val hashed_ABC =
    "fe153f639ec1439fad84263be471b79485d2356a52871798a68d221596f06cef" // ciHash("A+B+C")
  private val hashed_ABCD =
    "317c89a844216f56119b9ffa835390fe92e75a2c13f7d29026327bf3318560bd" // ciHash("A+B+C+D")

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
          WorkNode("B", 2, List("C", "D"), hashed_ABCD),
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

  describe("Update version") {
    it("processes an update for a newer version") {
      val existingVersion = 1
      val updateVersion = 2
      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("A", updateVersion, Set("B")),
          existingGraph =
            WorkGraph(Set(WorkNode("A", existingVersion, Nil, "A")))
        )
        .nodes should contain theSameElementsAs
        List(
          WorkNode("A", 2, List("B"), hashed_AB),
          WorkNode("B", 0, List(), hashed_AB))
    }

    it("doesn't process an update for a lower version") {
      val existingVersion = 3
      val updateVersion = 1

      val thrown = intercept[VersionExpectedConflictException] {
        WorkGraphUpdater
          .update(
            workUpdate = WorkUpdate("A", updateVersion, Set("B")),
            existingGraph =
              WorkGraph(Set(WorkNode("A", existingVersion, Nil, "A")))
          )
      }
      thrown.message shouldBe "update failed, work:A v1 is not newer than existing work v3"
    }

    it(
      "processes an update for the same version if it's the same as the one stored") {
      val existingVersion = 2
      val updateVersion = 2

      WorkGraphUpdater
        .update(
          workUpdate = WorkUpdate("A", updateVersion, Set("B")),
          existingGraph = WorkGraph(
            Set(
              WorkNode("A", existingVersion, List("B"), hashed_AB),
              WorkNode("B", 0, List(), hashed_AB)))
        )
        .nodes should contain theSameElementsAs
        List(
          WorkNode("A", 2, List("B"), hashed_AB),
          WorkNode("B", 0, List(), hashed_AB))
    }

    it(
      "doesn't process an update for the same version if the work is different from the one stored") {
      val existingVersion = 2
      val updateVersion = 2

      val thrown = intercept[VersionUnexpectedConflictException] {
        WorkGraphUpdater
          .update(
            workUpdate = WorkUpdate("A", updateVersion, Set("A")),
            existingGraph = WorkGraph(
              Set(
                WorkNode("A", existingVersion, List("B"), hashed_AB),
                WorkNode("B", 0, List(), hashed_AB)))
          )
      }
      thrown.getMessage shouldBe "update failed, work:A v2 already exists with different content! update-ids:Set(A) != existing-ids:Set(B)"
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
        WorkNode("A", 2, List("B"), hashed_AB),
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
        WorkNode("A", 2, List("B"), hashed_ABC),
        WorkNode("B", 3, List("C"), hashed_ABC),
        WorkNode("C", 1, Nil, hashed_ABC)
      )
    }
  }
}
