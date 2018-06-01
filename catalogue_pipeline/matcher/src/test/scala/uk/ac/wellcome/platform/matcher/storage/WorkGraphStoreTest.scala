package uk.ac.wellcome.platform.matcher.storage

import com.gu.scanamo.Scanamo
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.matcher.WorkNode
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.models.{WorkGraph, WorkUpdate}

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
            WorkUpdate("Not-there", 0, Set.empty))) { workGraph =>
              workGraph shouldBe WorkGraph(Set.empty)
          }
        }
      }
    }

    it(
      "returns a WorkNode if it has no links and it's the only node in the setId") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val work = WorkNode(workId = "A", version = 0, linkedIds = Nil, setId = "A")
          Scanamo.put(dynamoDbClient)(table.name)(work)

          whenReady(workGraphStore.findAffectedWorks(
            WorkUpdate("A", 0, Set.empty))) { linkedWorkGraph =>
            linkedWorkGraph shouldBe WorkGraph(Set(work))
          }
        }
      }
    }

    it("returns a WorkNode and the links in the workUpdate") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workA = WorkNode(workId = "A", version = 0, linkedIds = Nil, setId = "A")
          val workB = WorkNode(workId = "B", version = 0, linkedIds = Nil, setId = "B")
          Scanamo.put(dynamoDbClient)(table.name)(workA)
          Scanamo.put(dynamoDbClient)(table.name)(workB)

          whenReady(workGraphStore.findAffectedWorks(
            WorkUpdate("A", 0, Set("B")))) { workGraph =>
            workGraph.nodes shouldBe Set(workA, workB)
          }
        }
      }
    }

    it("returns a WorkNode and the links in the database") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>

          val workA =
            WorkNode(workId = "A", version = 0, linkedIds = List("B"), setId = "AB")
          val workB = WorkNode(workId = "B", version = 0, linkedIds = Nil, setId = "AB")
          Scanamo.put(dynamoDbClient)(table.name)(workA)
          Scanamo.put(dynamoDbClient)(table.name)(workB)

          whenReady(workGraphStore.findAffectedWorks(
            WorkUpdate("A", 0, Set.empty))) { workGraph =>
            workGraph.nodes shouldBe Set(workA, workB)
          }
        }
      }
    }

    it(
      "returns a WorkNode and the links in the database more than one level down") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workA =
            WorkNode(workId = "A", version = 0, linkedIds = List("B"), setId = "ABC")
          val workB =
            WorkNode(workId = "B", version = 0, linkedIds = List("C"), setId = "ABC")
          val workC = WorkNode(workId = "C", version = 0, linkedIds = Nil, setId = "ABC")
          Scanamo.put(WorkNode)(table.name)(workA)
          Scanamo.put(dynamoDbClient)(table.name)(workB)
          Scanamo.put(dynamoDbClient)(table.name)(workC)

          whenReady(workGraphStore.findAffectedWorks(
            WorkUpdate("A", 0, Set.empty))) { workGraph =>
            workGraph.nodes shouldBe Set(workA, workB, workC)
          }
        }
      }
    }

    it(
      "returns a WorkNode and the links in the database where an update joins two sets of works") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>

          val workA =
            WorkNode(workId = "A", version = 0, linkedIds = List("B"), setId = "AB")
          val workB = WorkNode(workId = "B", version = 0, linkedIds = Nil, setId = "AB")
          val workC = WorkNode(workId = "C", version = 0, linkedIds = Nil, setId = "C")

          Scanamo.put(dynamoDbClient)(table.name)(workA)
          Scanamo.put(dynamoDbClient)(table.name)(workB)
          Scanamo.put(dynamoDbClient)(table.name)(workC)

          whenReady(workGraphStore.findAffectedWorks(
            WorkUpdate("B", 0, Set("C")))) { workGraph =>
            workGraph.nodes shouldBe Set(workA, workB, workC)
          }
        }
      }
    }
  }

  describe("Put graph of linked works") {
    it("puts a simple graph") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workNodeA = WorkNode("A", version = 0, List("B"), "A+B")
          val workNodeB = WorkNode("B", version = 0, Nil, "A+B")

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
        val mockWorkNodeDao = mock[WorkNodeDao]
        val expectedException = new RuntimeException("FAILED")
        when(mockWorkNodeDao.put(any[WorkNode]))
          .thenReturn(Future.failed(expectedException))
        val workGraphStore = new WorkGraphStore(mockWorkNodeDao)

        whenReady(
          workGraphStore
            .put(WorkGraph(Set(WorkNode("A", version = 0, Nil, "A+B"))))
            .failed) { failedException =>
          failedException shouldBe expectedException
        }
      }
    }
  }
}
