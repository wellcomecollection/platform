package uk.ac.wellcome.platform.matcher.matcher

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.fixtures.{LocalLinkedWorkDynamoDb, MatcherFixtures}
import uk.ac.wellcome.platform.matcher.models._
import uk.ac.wellcome.platform.matcher.storage.{LinkedWorkDao, WorkGraphStore}
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.Future

class LinkedWorkMatcherTest
    extends FunSpec
    with Matchers
    with MatcherFixtures
    with LocalLinkedWorkDynamoDb
    with ScalaFutures
    with MockitoSugar {

  def withLinkedWorkMatcher[R](table: Table)(
    testWith: TestWith[LinkedWorkMatcher, R]): R = {
    val workGraphStore = new WorkGraphStore(
      new LinkedWorkDao(
        dynamoDbClient,
        DynamoConfig(table.name, Some(table.index))))
    val linkedWorkMatcher = new LinkedWorkMatcher(workGraphStore)
    testWith(linkedWorkMatcher)
  }

  def withLinkedWorkMatcher[R](table: Table, workGraphStore: WorkGraphStore)(
    testWith: TestWith[LinkedWorkMatcher, R]): R = {
    val linkedWorkMatcher = new LinkedWorkMatcher(workGraphStore)
    testWith(linkedWorkMatcher)
  }

  it(
    "matches a work entry with no linked identifiers to a matched works list referencing itself and saves the updated graph") {
    withLocalDynamoDbTable { table =>
      withLinkedWorkMatcher(table) { linkedWorkMatcher =>
        whenReady(linkedWorkMatcher.matchWork(anUnidentifiedSierraWork)) {
          identifiersList =>
            val workId = "sierra-system-number/id"
            identifiersList shouldBe
              LinkedWorksIdentifiersList(Set(IdentifierList(Set(workId))))

            val savedLinkedWork = Scanamo
              .get[LinkedWork](dynamoDbClient)(table.name)('workId -> workId)
              .map(_.right.get)
            savedLinkedWork shouldBe Some(LinkedWork(workId, Nil, workId))
        }
      }
    }
  }

  it(
    "matches a work entry with a linked identifier to a matched works list of identifiers") {
    withLocalDynamoDbTable { table =>
      withLinkedWorkMatcher(table) { linkedWorkMatcher =>
        val linkedIdentifier = aSierraSourceIdentifier("B")
        val aIdentifier = aSierraSourceIdentifier("A")
        val work = anUnidentifiedSierraWork.copy(
          sourceIdentifier = aIdentifier,
          identifiers = List(aIdentifier, linkedIdentifier))
        whenReady(linkedWorkMatcher.matchWork(work)) { identifiersList =>
          identifiersList shouldBe
            LinkedWorksIdentifiersList(
              Set(IdentifierList(
                Set("sierra-system-number/A", "sierra-system-number/B"))))

          val savedLinkedWorks = Scanamo
            .scan[LinkedWork](dynamoDbClient)(table.name)
            .map(_.right.get)
          savedLinkedWorks should contain theSameElementsAs List(
            LinkedWork(
              "sierra-system-number/A",
              List("sierra-system-number/B"),
              "sierra-system-number/A+sierra-system-number/B"),
            LinkedWork(
              "sierra-system-number/B",
              Nil,
              "sierra-system-number/A+sierra-system-number/B")
          )
        }
      }
    }
  }

  it("matches a work entry to a previously stored work") {
    withLocalDynamoDbTable { table =>
      withLinkedWorkMatcher(table) { linkedWorkMatcher =>
        val existingWorkA = LinkedWork(
          "sierra-system-number/A",
          List("sierra-system-number/B"),
          "sierra-system-number/A+sierra-system-number/B")
        val existingWorkB = LinkedWork(
          "sierra-system-number/B",
          Nil,
          "sierra-system-number/A+sierra-system-number/B")
        Scanamo.put(dynamoDbClient)(table.name)(existingWorkA)
        Scanamo.put(dynamoDbClient)(table.name)(existingWorkB)

        val bIdentifier = aSierraSourceIdentifier("B")
        val cIdentifier = aSierraSourceIdentifier("C")
        val work = anUnidentifiedSierraWork.copy(
          sourceIdentifier = bIdentifier,
          identifiers = List(bIdentifier, cIdentifier))

        whenReady(linkedWorkMatcher.matchWork(work)) { identifiersList =>
          identifiersList shouldBe
            LinkedWorksIdentifiersList(
              Set(
                IdentifierList(
                  Set(
                    "sierra-system-number/A",
                    "sierra-system-number/B",
                    "sierra-system-number/C"))))

          val savedLinkedWorks = Scanamo
            .scan[LinkedWork](dynamoDbClient)(table.name)
            .map(_.right.get)
          savedLinkedWorks should contain theSameElementsAs List(
            LinkedWork(
              "sierra-system-number/A",
              List("sierra-system-number/B"),
              "sierra-system-number/A+sierra-system-number/B+sierra-system-number/C"),
            LinkedWork(
              "sierra-system-number/B",
              List("sierra-system-number/C"),
              "sierra-system-number/A+sierra-system-number/B+sierra-system-number/C"),
            LinkedWork(
              "sierra-system-number/C",
              Nil,
              "sierra-system-number/A+sierra-system-number/B+sierra-system-number/C")
          )
        }
      }
    }
  }

  it("fails if saving the updated links fails") {
    withLocalDynamoDbTable { table =>
      val mockWorkGraphStore = mock[WorkGraphStore]
      withLinkedWorkMatcher(table, mockWorkGraphStore) { linkedWorkMatcher =>
        val expectedException = new RuntimeException("Failed to put")
        when(mockWorkGraphStore.findAffectedWorks(any[LinkedWorkUpdate]))
          .thenReturn(Future.successful(LinkedWorksGraph(Set.empty)))
        when(mockWorkGraphStore.put(any[LinkedWorksGraph]))
          .thenReturn(Future.failed(expectedException))
        whenReady(linkedWorkMatcher.matchWork(anUnidentifiedSierraWork).failed) {
          actualException =>
            actualException shouldBe expectedException
        }
      }
    }
  }
}
