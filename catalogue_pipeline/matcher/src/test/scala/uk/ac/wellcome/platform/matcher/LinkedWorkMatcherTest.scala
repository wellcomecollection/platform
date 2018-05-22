package uk.ac.wellcome.platform.matcher

import com.gu.scanamo.Scanamo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.fixtures.{LocalLinkedWorkDynamoDb, MatcherFixtures}
import uk.ac.wellcome.platform.matcher.models.{IdentifierList, LinkedWork, LinkedWorksIdentifiersList}
import uk.ac.wellcome.platform.matcher.storage.{DynamoConfig, LinkedWorkDao, WorkGraphStore}
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.test.fixtures.TestWith

class LinkedWorkMatcherTest
    extends FunSpec
    with Matchers
    with MatcherFixtures
    with LocalLinkedWorkDynamoDb
    with ScalaFutures {

  def withLinkedWorkMatcher[R](table: Table)(testWith: TestWith[LinkedWorkMatcher, R]): R = {
    val workGraphStore = new WorkGraphStore(new LinkedWorkDao(dynamoDbClient, DynamoConfig(table.name, table.index)))
    val linkedWorkMatcher = new LinkedWorkMatcher(workGraphStore)
    testWith(linkedWorkMatcher)
  }

  it("matches a work entry with no linked identifiers to a matched works list referencing itself") {
    withLocalDynamoDbTable { table =>
      withLinkedWorkMatcher(table) { linkedWorkMatcher =>
        whenReady(linkedWorkMatcher.matchWork(anUnidentifiedSierraWork)) { identifiersList =>
          identifiersList shouldBe
            LinkedWorksIdentifiersList(
              List(IdentifierList(Set("sierra-system-number/id"))))
        }
      }
    }
  }

  it("matches a work entry with a linked identifier to a matched works list of identifiers") {
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
              List(IdentifierList(
                Set("sierra-system-number/A", "sierra-system-number/B"))))
        }
      }
    }
  }

  it("matches a work entry to a previously stored work") {
    withLocalDynamoDbTable { table =>
      withLinkedWorkMatcher(table) { linkedWorkMatcher =>
        val existingWorkA = LinkedWork("sierra-system-number/A", List("sierra-system-number/B"), "sierra-system-number/A+sierra-system-number/B")
        val existingWorkB = LinkedWork("sierra-system-number/B", Nil, "sierra-system-number/A+sierra-system-number/B")
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
              List(IdentifierList(
                Set("sierra-system-number/A", "sierra-system-number/B", "sierra-system-number/C"))))
        }
      }
    }
  }
}
