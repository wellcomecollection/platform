package uk.ac.wellcome.platform.matcher.storage

import com.gu.scanamo.Scanamo
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.platform.matcher.fixtures.LocalLinkedWorkDynamoDb
import uk.ac.wellcome.platform.matcher.models.{
  LinkedWork,
  LinkedWorkUpdate,
  LinkedWorksGraph
}
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.Future

class WorkGraphStoreTest
    extends FunSpec
    with LocalLinkedWorkDynamoDb
    with Matchers
    with MockitoSugar
    with ScalaFutures {

  def withWorkGraphStore[R](table: Table)(
    testWith: TestWith[WorkGraphStore, R]): R = {
    val workGraphStore = new WorkGraphStore(
      new LinkedWorkDao(
        dynamoDbClient,
        MatcherDynamoConfig(table.name, table.index)))
    testWith(workGraphStore)
  }

  describe("Get graph of linked works") {
    it("returns nothing if there are no matching graphs") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          whenReady(
            workGraphStore.findAffectedWorks(
              LinkedWorkUpdate("Not-there", Set.empty))) { linkedWorkGraph =>
            linkedWorkGraph shouldBe LinkedWorksGraph(Set.empty)
          }
        }
      }
    }

    it(
      "returns a LinkedWork if it has no links and it's the only node in the setId") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val work = LinkedWork(workId = "A", linkedIds = Nil, setId = "A")
          Scanamo.put(dynamoDbClient)(table.name)(work)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("A", Set.empty))) { linkedWorkGraph =>
            linkedWorkGraph shouldBe LinkedWorksGraph(Set(work))
          }
        }
      }
    }

    it("returns a LinkedWork and the links in the workUpdate") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workA = LinkedWork(workId = "A", linkedIds = Nil, setId = "A")
          val workB = LinkedWork(workId = "B", linkedIds = Nil, setId = "B")
          Scanamo.put(dynamoDbClient)(table.name)(workA)
          Scanamo.put(dynamoDbClient)(table.name)(workB)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("A", Set("B")))) { linkedWorkGraph =>
            linkedWorkGraph.linkedWorksSet shouldBe Set(workA, workB)
          }
        }
      }
    }

    it("returns a LinkedWork and the links in the database") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workA =
            LinkedWork(workId = "A", linkedIds = List("B"), setId = "AB")
          val workB = LinkedWork(workId = "B", linkedIds = Nil, setId = "AB")
          Scanamo.put(dynamoDbClient)(table.name)(workA)
          Scanamo.put(dynamoDbClient)(table.name)(workB)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("A", Set.empty))) { linkedWorkGraph =>
            linkedWorkGraph.linkedWorksSet shouldBe Set(workA, workB)
          }
        }
      }
    }

    it(
      "returns a LinkedWork and the links in the database more than one level down") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workA =
            LinkedWork(workId = "A", linkedIds = List("B"), setId = "ABC")
          val workB =
            LinkedWork(workId = "B", linkedIds = List("C"), setId = "ABC")
          val workC = LinkedWork(workId = "C", linkedIds = Nil, setId = "ABC")
          Scanamo.put(dynamoDbClient)(table.name)(workA)
          Scanamo.put(dynamoDbClient)(table.name)(workB)
          Scanamo.put(dynamoDbClient)(table.name)(workC)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("A", Set.empty))) { linkedWorkGraph =>
            linkedWorkGraph.linkedWorksSet shouldBe Set(workA, workB, workC)
          }
        }
      }
    }

    it(
      "returns a LinkedWork and the links in the database where an update joins two sets of works") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workA =
            LinkedWork(workId = "A", linkedIds = List("B"), setId = "AB")
          val workB = LinkedWork(workId = "B", linkedIds = Nil, setId = "AB")
          val workC = LinkedWork(workId = "C", linkedIds = Nil, setId = "C")
          Scanamo.put(dynamoDbClient)(table.name)(workA)
          Scanamo.put(dynamoDbClient)(table.name)(workB)
          Scanamo.put(dynamoDbClient)(table.name)(workC)

          whenReady(workGraphStore.findAffectedWorks(
            LinkedWorkUpdate("B", Set("C")))) { linkedWorkGraph =>
            linkedWorkGraph.linkedWorksSet shouldBe Set(workA, workB, workC)
          }
        }
      }
    }
  }

  describe("Put graph of linked works") {
    it("puts a simple graph") {
      withLocalDynamoDbTable { table =>
        withWorkGraphStore(table) { workGraphStore =>
          val workA = LinkedWork("A", List("B"), "A+B")
          val workB = LinkedWork("B", Nil, "A+B")

          whenReady(workGraphStore.put(LinkedWorksGraph(Set(workA, workB)))) {
            _ =>
              val savedLinkedWorks = Scanamo
                .scan[LinkedWork](dynamoDbClient)(table.name)
                .map(_.right.get)
              savedLinkedWorks should contain theSameElementsAs List(
                workA,
                workB)
          }
        }
      }
    }

    it("throws if linkedDao fails to put") {
      withLocalDynamoDbTable { table =>
        val mockLinkedWorkDao = mock[LinkedWorkDao]
        val expectedException = new RuntimeException("FAILED")
        when(mockLinkedWorkDao.put(any[LinkedWork]))
          .thenReturn(Future.failed(expectedException))
        val workGraphStore = new WorkGraphStore(mockLinkedWorkDao)

        whenReady(
          workGraphStore
            .put(LinkedWorksGraph(Set(LinkedWork("A", Nil, "A+B"))))
            .failed) { failedException =>
          failedException shouldBe expectedException
        }
      }
    }
  }

}
