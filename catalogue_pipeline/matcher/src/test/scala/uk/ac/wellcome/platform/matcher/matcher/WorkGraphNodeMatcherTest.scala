package uk.ac.wellcome.platform.matcher.matcher

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.matcher.{MatchedIdentifiers, MatcherResult, WorkIdentifier, WorkNode}
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.models.{WorkGraph, WorkUpdate}
import uk.ac.wellcome.platform.matcher.storage.WorkGraphStore
import org.mockito.Matchers.any
import org.mockito.Mockito.when

import scala.concurrent.Future

class WorkGraphNodeMatcherTest
    extends FunSpec
    with Matchers
    with MatcherFixtures
    with ScalaFutures
    with MockitoSugar {

  it("matches a work entry with no linked identifiers to a matched works list referencing itself and saves the updated graph") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { graphTable =>
        withWorkGraphStore(graphTable, lockTable) { workGraphStore =>
          withWorkMatcher(workGraphStore) { linkedWorkMatcher =>
            whenReady(linkedWorkMatcher.matchWork(anUnidentifiedSierraWork)) {
              matcherResult =>
                val workId = "sierra-system-number/id"

                matcherResult shouldBe
                  MatcherResult(
                    Set(MatchedIdentifiers(Set(WorkIdentifier(workId, 1)))))

                val savedLinkedWork = Scanamo
                  .get[WorkNode](dynamoDbClient)(graphTable.name)('id -> workId)
                  .map(_.right.get)

                savedLinkedWork shouldBe Some(WorkNode(workId, 1, Nil, workId))
            }
          }
        }
      }
    }
  }

  it(
    "matches a work entry with a linked identifier to a matched works list of identifiers") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { graphTable =>
        withWorkGraphStore(graphTable, lockTable) { workGraphStore =>
          withWorkMatcher(workGraphStore) { linkedWorkMatcher =>
            val identifierA = aSierraSourceIdentifier("A")
            val identifierB = aSierraSourceIdentifier("B")
            val work = anUnidentifiedSierraWork.copy(
              sourceIdentifier = identifierA,
              identifiers = List(identifierA, identifierB))
            whenReady(linkedWorkMatcher.matchWork(work)) { identifiersList =>
              identifiersList shouldBe
                MatcherResult(
                  Set(
                    MatchedIdentifiers(Set(
                      WorkIdentifier("sierra-system-number/A", 1),
                      WorkIdentifier("sierra-system-number/B", 0)))))

              val savedWorkNodes = Scanamo
                .scan[WorkNode](dynamoDbClient)(graphTable.name)
                .map(_.right.get)

              savedWorkNodes should contain theSameElementsAs List(
                WorkNode(
                  "sierra-system-number/A",
                  1,
                  List("sierra-system-number/B"),
                  "sierra-system-number/A+sierra-system-number/B"),
                WorkNode(
                  "sierra-system-number/B",
                  0,
                  Nil,
                  "sierra-system-number/A+sierra-system-number/B")
              )
            }
          }
        }
      }
    }
  }

  it("matches a work entry to a previously stored work") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { graphTable =>
        withWorkGraphStore(graphTable, lockTable) { workGraphStore =>
          withWorkMatcher(workGraphStore) { linkedWorkMatcher =>
            val existingWorkA = WorkNode(
              "sierra-system-number/A",
              1,
              List("sierra-system-number/B"),
              "sierra-system-number/A+sierra-system-number/B")
            val existingWorkB = WorkNode(
              "sierra-system-number/B",
              1,
              Nil,
              "sierra-system-number/A+sierra-system-number/B")
            val existingWorkC = WorkNode(
              "sierra-system-number/C",
              1,
              Nil,
              "sierra-system-number/C")
            Scanamo.put(dynamoDbClient)(graphTable.name)(existingWorkA)
            Scanamo.put(dynamoDbClient)(graphTable.name)(existingWorkB)
            Scanamo.put(dynamoDbClient)(graphTable.name)(existingWorkC)

            val bIdentifier = aSierraSourceIdentifier("B")
            val cIdentifier = aSierraSourceIdentifier("C")
            val work = anUnidentifiedSierraWork.copy(
              sourceIdentifier = bIdentifier,
              version = 2,
              identifiers = List(bIdentifier, cIdentifier))

            whenReady(linkedWorkMatcher.matchWork(work)) { identifiersList =>
              identifiersList shouldBe
                MatcherResult(
                  Set(
                    MatchedIdentifiers(
                      Set(
                        WorkIdentifier("sierra-system-number/A", 1),
                        WorkIdentifier("sierra-system-number/B", 2),
                        WorkIdentifier("sierra-system-number/C", 1)))))

              val savedNodes = Scanamo
                .scan[WorkNode](dynamoDbClient)(graphTable.name)
                .map(_.right.get)

              savedNodes should contain theSameElementsAs List(
                WorkNode(
                  "sierra-system-number/A",
                  1,
                  List("sierra-system-number/B"),
                  "sierra-system-number/A+sierra-system-number/B+sierra-system-number/C"),
                WorkNode(
                  "sierra-system-number/B",
                  2,
                  List("sierra-system-number/C"),
                  "sierra-system-number/A+sierra-system-number/B+sierra-system-number/C"),
                WorkNode(
                  "sierra-system-number/C",
                  1,
                  Nil,
                  "sierra-system-number/A+sierra-system-number/B+sierra-system-number/C")
              )
            }
          }
        }
      }
    }
  }

  it("fails if saving the updated links fails") {
    val mockWorkGraphStore = mock[WorkGraphStore]
    withWorkMatcher(mockWorkGraphStore) { linkedWorkMatcher =>
      val expectedException = new RuntimeException("Failed to put")
      when(mockWorkGraphStore.findAffectedWorks(any[WorkUpdate]))
        .thenReturn(Future.successful(WorkGraph(Set.empty)))
      when(mockWorkGraphStore.put(any[WorkGraph], any[WorkGraph]))
        .thenThrow(expectedException)

      linkedWorkMatcher.matchWork(anUnidentifiedSierraWork)

      whenReady(linkedWorkMatcher.matchWork(anUnidentifiedSierraWork).failed) {
        actualException =>
          actualException shouldBe expectedException
      }
    }
  }
}
