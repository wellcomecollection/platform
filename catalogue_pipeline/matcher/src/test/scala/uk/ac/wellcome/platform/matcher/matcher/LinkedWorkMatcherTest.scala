package uk.ac.wellcome.platform.matcher.matcher

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.models._
import uk.ac.wellcome.platform.matcher.storage.WorkGraphStore

import scala.concurrent.Future

class LinkedWorkMatcherTest
    extends FunSpec
    with Matchers
    with MatcherFixtures
    with ScalaFutures
    with MockitoSugar {

  it(
    "matches a work entry with no linked identifiers to a matched works list referencing itself and saves the updated graph") {
    withLocalDynamoDbTable { table =>
      withWorkGraphStore(table) { workGraphStore =>
        withLinkedWorkMatcher(table, workGraphStore) { linkedWorkMatcher =>
          whenReady(linkedWorkMatcher.matchWork(anUnidentifiedSierraWork)) {
            identifiersList =>
              val workId = "sierra-system-number/id"
              identifiersList shouldBe
                LinkedWorksIdentifiersList(Set(IdentifierList(Set(workId))))

              val savedLinkedWork = Scanamo
                .get[WorkNode](dynamoDbClient)(table.name)('id -> workId)
                .map(_.right.get)
              savedLinkedWork shouldBe Some(WorkNode(workId, Nil, workId))
          }
        }
      }
    }
  }

  it(
    "matches a work entry with a linked identifier to a matched works list of identifiers") {
    withLocalDynamoDbTable { table =>
      withWorkGraphStore(table) { workGraphStore =>
        withLinkedWorkMatcher(table, workGraphStore) { linkedWorkMatcher =>
          val linkedIdentifier = aSierraSourceIdentifier("B")
          val aIdentifier = aSierraSourceIdentifier("A")
          val work = anUnidentifiedSierraWork.copy(
            sourceIdentifier = aIdentifier,
            identifiers = List(aIdentifier, linkedIdentifier))
          whenReady(linkedWorkMatcher.matchWork(work)) { identifiersList =>
            identifiersList shouldBe
              LinkedWorksIdentifiersList(Set(IdentifierList(
                Set("sierra-system-number/A", "sierra-system-number/B"))))

            val savedWorkNodes = Scanamo
              .scan[WorkNode](dynamoDbClient)(table.name)
              .map(_.right.get)
            savedWorkNodes should contain theSameElementsAs List(
              WorkNode(
                "sierra-system-number/A",
                List("sierra-system-number/B"),
                "sierra-system-number/A+sierra-system-number/B"),
              WorkNode(
                "sierra-system-number/B",
                Nil,
                "sierra-system-number/A+sierra-system-number/B")
            )
          }
        }
      }
    }
  }

  it("matches a work entry to a previously stored work") {
    withLocalDynamoDbTable { table =>
      withWorkGraphStore(table) { workGraphStore =>
        withLinkedWorkMatcher(table, workGraphStore) { linkedWorkMatcher =>
          val existingWorkA = WorkNode(
            "sierra-system-number/A",
            List("sierra-system-number/B"),
            "sierra-system-number/A+sierra-system-number/B")
          val existingWorkB = WorkNode(
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

            val savedWorkNodes = Scanamo
              .scan[WorkNode](dynamoDbClient)(table.name)
              .map(_.right.get)
            savedWorkNodes should contain theSameElementsAs List(
              WorkNode(
                "sierra-system-number/A",
                List("sierra-system-number/B"),
                "sierra-system-number/A+sierra-system-number/B+sierra-system-number/C"),
              WorkNode(
                "sierra-system-number/B",
                List("sierra-system-number/C"),
                "sierra-system-number/A+sierra-system-number/B+sierra-system-number/C"),
              WorkNode(
                "sierra-system-number/C",
                Nil,
                "sierra-system-number/A+sierra-system-number/B+sierra-system-number/C")
            )
          }
        }
      }
    }
  }

  it("fails if saving the updated links fails") {
    withLocalDynamoDbTable { table =>
      val mockWorkGraphStore = mock[WorkGraphStore]
      withLinkedWorkMatcher(table, mockWorkGraphStore) { linkedWorkMatcher =>
        val expectedException = new RuntimeException("Failed to put")
        when(mockWorkGraphStore.findAffectedWorks(any[WorkUpdate]))
          .thenReturn(Future.successful(WorkGraph(Set.empty)))
        when(mockWorkGraphStore.put(any[WorkGraph]))
          .thenReturn(Future.failed(expectedException))
        whenReady(linkedWorkMatcher.matchWork(anUnidentifiedSierraWork).failed) {
          actualException =>
            actualException shouldBe expectedException
        }
      }
    }
  }
}
