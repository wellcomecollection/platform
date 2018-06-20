package uk.ac.wellcome.platform.matcher.matcher

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.matcher.{MatchedIdentifiers, MatcherResult, WorkIdentifier, WorkNode}
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.lockable.Identifier
import uk.ac.wellcome.platform.matcher.models.{WorkGraph, WorkUpdate}
import uk.ac.wellcome.platform.matcher.storage.WorkGraphStore

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WorkMatcherTest
    extends FunSpec
    with Matchers
    with MatcherFixtures
    with ScalaFutures
    with MockitoSugar {

  it(
    "matches a work with no linked identifiers to itself only A and saves the updated graph A") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { graphTable =>
        withWorkGraphStore(graphTable) { workGraphStore =>
          withWorkMatcher(workGraphStore, lockTable) { workMatcher =>
            whenReady(workMatcher.matchWork(anUnidentifiedSierraWork)) {
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
    "matches a work with a single linked identifier A->B and saves the graph A->B") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { graphTable =>
        withWorkGraphStore(graphTable) { workGraphStore =>
          withWorkMatcher(workGraphStore, lockTable) { workMatcher =>
            val identifierA = aSierraSourceIdentifier("A")
            val identifierB = aSierraSourceIdentifier("B")
            val work = anUnidentifiedSierraWork.copy(
              sourceIdentifier = identifierA,
              otherIdentifiers = List(identifierB)
            )
            whenReady(workMatcher.matchWork(work)) { identifiersList =>
              identifiersList shouldBe
                MatcherResult(
                  Set(MatchedIdentifiers(Set(
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

  it(
    "matches a previously stored work A->B with an update B->C and saves the graph A->B->C") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { graphTable =>
        withWorkGraphStore(graphTable) { workGraphStore =>
          withWorkMatcher(workGraphStore, lockTable) { workMatcher =>
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
              otherIdentifiers = List(cIdentifier)
            )

            whenReady(workMatcher.matchWork(work)) { identifiersList =>
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

  it("throws GracefulFailureException if it fails to lock primary works") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { graphTable =>
        withWorkGraphStore(graphTable) { workGraphStore =>
          withDynamoRowLockDao(dynamoDbClient, lockTable) { rowLockDao =>
            withLockingService(rowLockDao) { dynamoLockingService =>
              withWorkMatcherAndLockingService(workGraphStore, dynamoLockingService) { workMatcher =>

                val failedLock = for {
                  _ <- rowLockDao.lockRow(
                    Identifier("sierra-system-number/id"),
                    "processId")
                  result <- workMatcher.matchWork(anUnidentifiedSierraWork.copy())
                } yield result

                whenReady(failedLock.failed) { failedMatch =>
                  failedMatch shouldBe a[GracefulFailureException]
                }

              }
            }
          }
        }
      }
    }
  }

  it("throws GracefulFailureException if it fails to lock secondary works") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { graphTable =>
        withWorkGraphStore(graphTable) { workGraphStore =>
          withDynamoRowLockDao(dynamoDbClient, lockTable) { rowLockDao =>
            withLockingService(rowLockDao) { dynamoLockingService =>
              withWorkMatcherAndLockingService(workGraphStore, dynamoLockingService) { workMatcher =>

                val identifierA = aSierraSourceIdentifier("A")
                val identifierB = aSierraSourceIdentifier("B")

                // A->B->C
                workGraphStore.put(WorkGraph(Set(
                  WorkNode("sierra-system-number/A", 0, List("sierra-system-number/B"), "sierra-system-number/A+sierra-system-number/B+sierra-system-number/C"),
                  WorkNode("sierra-system-number/B", 0, List("sierra-system-number/C"), "sierra-system-number/A+sierra-system-number/B+sierra-system-number/C"),
                  WorkNode("sierra-system-number/C", 0, Nil,                            "sierra-system-number/A+sierra-system-number/B+sierra-system-number/C")
                )))

                val work = anUnidentifiedSierraWork.copy(
                  sourceIdentifier = identifierA,
                  identifiers = List(identifierA, identifierB)
                )
                val failedLock = for {
                  _ <- rowLockDao.lockRow(
                    Identifier("sierra-system-number/C"),
                    "processId")
                  result <- workMatcher.matchWork(work)
                } yield result

                whenReady(failedLock.failed) { failedMatch =>
                  failedMatch shouldBe a[GracefulFailureException]
                }
              }
            }
          }
        }
      }
    }
  }

  it("fails if saving the updated links fails") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      val mockWorkGraphStore = mock[WorkGraphStore]
      withWorkMatcher(mockWorkGraphStore, lockTable) { workMatcher =>
        val expectedException = new RuntimeException("Failed to put")
        when(mockWorkGraphStore.findAffectedWorks(any[WorkUpdate]))
          .thenReturn(Future.successful(WorkGraph(Set.empty)))
        when(mockWorkGraphStore.put(any[WorkGraph]))
          .thenThrow(expectedException)

        whenReady(workMatcher.matchWork(anUnidentifiedSierraWork).failed) {
          actualException =>
            actualException shouldBe expectedException
        }
      }
    }
  }
}
