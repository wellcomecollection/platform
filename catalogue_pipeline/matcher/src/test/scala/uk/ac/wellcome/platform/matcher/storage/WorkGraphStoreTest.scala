package uk.ac.wellcome.platform.matcher.storage

import com.gu.scanamo.Scanamo
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.models.{
  LinkedWorkUpdate,
  WorkGraph,
  WorkNode
}

import scala.concurrent.Future

class WorkGraphStoreTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with MatcherFixtures {

  describe("Get graph of linked works") {
    it("returns nothing if there are no matching graphs") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          whenReady(
            workGraphStore.findAffectedWorks(
              LinkedWorkUpdate("Not-there", Set.empty))) { workGraph =>
            workGraph shouldBe WorkGraph(Set.empty)
          }
        }
      }
    }

    it(
      "returns a WorkNode if it has no links and it's the only node in the setId") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workNode = WorkNode("A", Nil, "A")
          Scanamo.put(dynamoDbClient)(table.name)(workNode)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("A", Set.empty))) { workGraph =>
            workGraph shouldBe WorkGraph(Set(workNode))
          }
        }
      }
    }

    it("returns a WorkNode and the links in the workUpdate") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workNodeA = WorkNode("A", Nil, "A")
          val workNodeB = WorkNode("B", Nil, "B")
          Scanamo.put(dynamoDbClient)(table.name)(workNodeA)
          Scanamo.put(dynamoDbClient)(table.name)(workNodeB)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("A", Set("B")))) { workGraph =>
            workGraph.nodes shouldBe Set(workNodeA, workNodeB)
          }
        }
      }
    }

    it("returns a WorkNode and the links in the database") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workNodeA = WorkNode("A", List("B"), "AB")
          val workNodeB = WorkNode("B", Nil, "AB")
          Scanamo.put(dynamoDbClient)(table.name)(workNodeA)
          Scanamo.put(dynamoDbClient)(table.name)(workNodeB)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("A", Set.empty))) { workGraph =>
            workGraph.nodes shouldBe Set(workNodeA, workNodeB)
          }
        }
      }
    }

    it(
      "returns a WorkNode and the links in the database more than one level down") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workNodeA = WorkNode("A", List("B"), "ABC")
          val workNodeB = WorkNode("B", List("C"), "ABC")
          val workNodeC = WorkNode("C", Nil, "ABC")
          Scanamo.put(dynamoDbClient)(table.name)(workNodeA)
          Scanamo.put(dynamoDbClient)(table.name)(workNodeB)
          Scanamo.put(dynamoDbClient)(table.name)(workNodeC)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("A", Set.empty))) { workGraph =>
            workGraph.nodes shouldBe Set(workNodeA, workNodeB, workNodeC)
          }
        }
      }
    }

    it(
      "returns a WorkNode and the links in the database where an update joins two sets of works") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workNodeA = WorkNode("A", List("B"), "AB")
          val workNodeB = WorkNode("B", Nil, "AB")
          val workNodeC = WorkNode("C", Nil, "C")
          Scanamo.put(dynamoDbClient)(table.name)(workNodeA)
          Scanamo.put(dynamoDbClient)(table.name)(workNodeB)
          Scanamo.put(dynamoDbClient)(table.name)(workNodeC)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("B", Set("C")))) { workGraph =>
            workGraph.nodes shouldBe Set(workNodeA, workNodeB, workNodeC)
          }
        }
      }
    }
  }

  describe("Put graph of linked works") {
    it("puts a simple graph") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workNodeA = WorkNode("A", List("B"), "A+B")
          val workNodeB = WorkNode("B", Nil, "A+B")

          whenReady(workGraphStore.put(WorkGraph(Set(workNodeA, workNodeB)))) {
            _ =>
              val savedLinkedWorks = Scanamo
                .scan[WorkNode](dynamoDbClient)(table.name)
                .map(_.right.get)
              savedLinkedWorks should contain theSameElementsAs List(
                workNodeA,
                workNodeB)
          }
        }
      }
    }

    it("throws if linkedDao fails to put") {
      withLocalDynamoDbTable { table =>
        val mockLinkedWorkDao = mock[LinkedWorkDao]
        val expectedException = new RuntimeException("FAILED")
        when(mockLinkedWorkDao.put(any[WorkNode]))
          .thenReturn(Future.failed(expectedException))
        val workGraphStore = new WorkGraphStore(mockLinkedWorkDao)

        whenReady(
          workGraphStore
            .put(WorkGraph(Set(WorkNode("A", Nil, "A+B"))))
            .failed) { failedException =>
          failedException shouldBe expectedException
        }
      }
    }
  }

}
