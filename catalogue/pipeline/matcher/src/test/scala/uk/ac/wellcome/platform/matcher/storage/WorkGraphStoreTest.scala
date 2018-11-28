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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
class WorkGraphStoreTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with MatcherFixtures {

  describe("Get graph of linked works") {
    it("returns nothing if there are no matching graphs") {
      withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { thingTable =>
        withWorkGraphStore(thingTable) { workGraphStore =>
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
      withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { thingTable =>
        withWorkGraphStore(thingTable) { workGraphStore =>
          val work =
            WorkNode(id = "A", version = 0, linkedIds = Nil, componentId = "A")
          Scanamo.put(dynamoDbClient)(thingTable.name)(work)

          whenReady(
            workGraphStore.findAffectedWorks(WorkUpdate("A", 0, Set.empty))) {
            workGraph =>
              workGraph shouldBe WorkGraph(Set(work))
          }
        }
      }
    }

    it("returns a WorkNode and the links in the workUpdate") {
      withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { thingTable =>
        withWorkGraphStore(thingTable) { workGraphStore =>
          val workA =
            WorkNode(id = "A", version = 0, linkedIds = Nil, componentId = "A")
          val workB =
            WorkNode(id = "B", version = 0, linkedIds = Nil, componentId = "B")
          Scanamo.put(dynamoDbClient)(thingTable.name)(workA)
          Scanamo.put(dynamoDbClient)(thingTable.name)(workB)

          whenReady(
            workGraphStore.findAffectedWorks(WorkUpdate("A", 0, Set("B")))) {
            workGraph =>
              workGraph.nodes shouldBe Set(workA, workB)
          }
        }
      }
    }

    it("returns a WorkNode and the links in the database") {
      withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { thingTable =>
        withWorkGraphStore(thingTable) { workGraphStore =>
          val workA =
            WorkNode(
              id = "A",
              version = 0,
              linkedIds = List("B"),
              componentId = "AB")
          val workB =
            WorkNode(id = "B", version = 0, linkedIds = Nil, componentId = "AB")

          Scanamo.put(dynamoDbClient)(thingTable.name)(workA)
          Scanamo.put(dynamoDbClient)(thingTable.name)(workB)

          whenReady(
            workGraphStore.findAffectedWorks(WorkUpdate("A", 0, Set.empty))) {
            workGraph =>
              workGraph.nodes shouldBe Set(workA, workB)
          }
        }
      }
    }

    it(
      "returns a WorkNode and the links in the database more than one level down") {
      withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { thingTable =>
        withWorkGraphStore(thingTable) { workGraphStore =>
          val workA =
            WorkNode(
              id = "A",
              version = 0,
              linkedIds = List("B"),
              componentId = "ABC")
          val workB =
            WorkNode(
              id = "B",
              version = 0,
              linkedIds = List("C"),
              componentId = "ABC")
          val workC = WorkNode(
            id = "C",
            version = 0,
            linkedIds = Nil,
            componentId = "ABC")

          Scanamo.put(dynamoDbClient)(thingTable.name)(workA)
          Scanamo.put(dynamoDbClient)(thingTable.name)(workB)
          Scanamo.put(dynamoDbClient)(thingTable.name)(workC)

          whenReady(
            workGraphStore.findAffectedWorks(WorkUpdate("A", 0, Set.empty))) {
            workGraph =>
              workGraph.nodes shouldBe Set(workA, workB, workC)
          }
        }
      }
    }

    it(
      "returns a WorkNode and the links in the database where an update joins two sets of works") {
      withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { thingTable =>
        withWorkGraphStore(thingTable) { workGraphStore =>
          val workA =
            WorkNode(
              id = "A",
              version = 0,
              linkedIds = List("B"),
              componentId = "AB")
          val workB =
            WorkNode(id = "B", version = 0, linkedIds = Nil, componentId = "AB")
          val workC =
            WorkNode(id = "C", version = 0, linkedIds = Nil, componentId = "C")

          Scanamo.put(dynamoDbClient)(thingTable.name)(workA)
          Scanamo.put(dynamoDbClient)(thingTable.name)(workB)
          Scanamo.put(dynamoDbClient)(thingTable.name)(workC)

          whenReady(
            workGraphStore.findAffectedWorks(WorkUpdate("B", 0, Set("C")))) {
            workGraph =>
              workGraph.nodes shouldBe Set(workA, workB, workC)
          }
        }
      }
    }
  }

  describe("Put graph of linked works") {
    it("puts a simple graph") {
      withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { graphTable =>
        withWorkGraphStore(graphTable) { workGraphStore =>
          val workNodeA = WorkNode("A", version = 0, List("B"), "A+B")
          val workNodeB = WorkNode("B", version = 0, Nil, "A+B")

          whenReady(workGraphStore.put(WorkGraph(Set(workNodeA, workNodeB)))) {
            _ =>
              val savedWorks = Scanamo
                .scan[WorkNode](dynamoDbClient)(graphTable.name)
                .map(_.right.get)
              savedWorks should contain theSameElementsAs List(
                workNodeA,
                workNodeB)
          }
        }
      }
    }
  }

  it("throws a RuntimeException if workGraphStore fails to put") {
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
