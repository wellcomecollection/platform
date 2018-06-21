package uk.ac.wellcome.platform.matcher.matcher

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.matcher.MatcherResult
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WorkMatcherTest
  extends FunSpec
    with Matchers
    with MatcherFixtures
    with ScalaFutures
    with MockitoSugar {

  it("processes one of two conflicting concurrent updates and locks the other") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { graphTable =>
        withWorkGraphStore(graphTable) { workGraphStore =>
          withDynamoRowLockDao(dynamoDbClient, lockTable) { rowLockDao =>
            withLockingService(rowLockDao) { dynamoLockingService =>
              withWorkMatcherAndLockingService(workGraphStore, dynamoLockingService) { workMatcher =>

                val identifierA = aSierraSourceIdentifier("A")
                val identifierB = aSierraSourceIdentifier("B")

                val workA = anUnidentifiedSierraWork.copy(
                  sourceIdentifier = identifierA,
                  identifiers = List(identifierA, identifierB)
                )

                val workB = anUnidentifiedSierraWork.copy(
                  sourceIdentifier = identifierB,
                  identifiers = List(identifierB)
                )

                val eventualResultA = workMatcher.matchWork(workA)
                val eventualResultB = workMatcher.matchWork(workB)

                val eventualResults = for {
                  resultA <- eventualResultA recoverWith {case e: GracefulFailureException => Future.successful(e)}
                  resultB <- eventualResultB recoverWith {case e: GracefulFailureException => Future.successful(e)}
                } yield (resultA, resultB)

                whenReady(eventualResults) { results =>
                  val resultsList = results.productIterator.toList
                  val failure = resultsList.collect({ case e: GracefulFailureException => e })
                  val result = resultsList.collect({ case r: MatcherResult => r })

                  failure.size shouldBe 1
                  result.size shouldBe 1

                  assertNoRowLocks(lockTable)
                }

              }
            }
          }
        }
      }
    }
  }

}