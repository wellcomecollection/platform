package uk.ac.wellcome.platform.matcher.locking
import java.time.Instant

import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.{DynamoFormat, Scanamo}
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.lockable.{FailedLockException, RowLock}
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DynamoLockingServiceTest
    extends FunSpec
    with MatcherFixtures
    with ScalaFutures {

  implicit val instantLongFormat: AnyRef with DynamoFormat[Instant] =
    DynamoFormat.coercedXmap[Instant, Long, IllegalArgumentException](
      Instant.ofEpochSecond
    )(
      _.getEpochSecond
    )

  it("locks around a callback") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withDynamoRowLockDao(lockTable) { dynamoRowLockDao =>
        withLockingService(dynamoRowLockDao) { lockingService =>
          val id = "id"
          val lockedDuringCallback =
            lockingService.withLocks(Set(id))(f = Future {
              assertOnlyHaveRowLockRecordIds(Set(id), lockTable)
            })

          whenReady(lockedDuringCallback) { _ =>
            assertNoRowLocks(lockTable)
          }
        }
      }
    }
  }

  it(
    "throws a FailedLockException and releases locks when writing a row lock fails") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withDynamoRowLockDao(lockTable) { dynamoRowLockDao =>
        withLockingService(dynamoRowLockDao) { lockingService =>
          val idA = "id"
          val lockedId = "lockedId"
          givenLocks(Set(lockedId), "existingContext", lockTable)

          val eventuallyLockFails =
            lockingService.withLocks(Set(idA, lockedId))(f = Future {
              fail("Lock did not fail")
            })

          whenReady(eventuallyLockFails.failed) { failure =>
            failure shouldBe a[FailedLockException]
            // still expect original locks to exist
            assertOnlyHaveRowLockRecordIds(Set(lockedId), lockTable)
          }
        }
      }
    }
  }

//  it("throws a FailedUnlockException when unlocking the context fails") {
//    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
//      withLockingService(lockTable) { lockingService =>
//        val idA = "id"
//        val lockedId = "lockedId"
//        givenLocks(Set(lockedId), "contextId", lockTable)
//
//        val eventuallyLockFails = lockingService.withLocks(Set(idA, lockedId))(f = Future {
//          fail("Lock did not fail")
//        })
//
//        whenReady(eventuallyLockFails.failed) { failure =>
//          failure shouldBe a[FailedLockException]
//          assertNoRowLocks(lockTable)
//        }
//      }
//    }
//  }

  it("releases locks when the callback fails") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withDynamoRowLockDao(lockTable) { dynamoRowLockDao =>
        withLockingService(dynamoRowLockDao) { lockingService =>
          case class ExpectedException() extends Exception()

          val id = "id"
          val eventuallyLockFails =
            lockingService.withLocks(Set(id))(f = Future {
              assertOnlyHaveRowLockRecordIds(Set(id), lockTable)
              throw new ExpectedException
            })

          whenReady(eventuallyLockFails.failed) { failure =>
            failure shouldBe a[ExpectedException]
            assertNoRowLocks(lockTable)
          }
        }
      }
    }
  }

  private def givenLocks(ids: Set[String],
                         contextId: String,
                         lockTable: LocalDynamoDb.Table) = {
    ids.foreach(
      id =>
        Scanamo.put[RowLock](dynamoDbClient)(lockTable.name)(
          aRowLock(id, contextId)))
  }

  private def aRowLock(id: String, contextId: String) = {
    RowLock(id, contextId, Instant.now, Instant.now.plusSeconds(100))
  }

  private def assertOnlyHaveRowLockRecordIds(
    expectedIds: Set[String],
    lockTable: LocalDynamoDb.Table): Any = {
    val locks: immutable.Seq[Either[DynamoReadError, RowLock]] =
      Scanamo.scan[RowLock](dynamoDbClient)(lockTable.name)
    val actualIds = locks.map(lock => lock.right.get.id).toSet
    actualIds shouldBe expectedIds
  }

  private def assertNoRowLocks(lockTable: LocalDynamoDb.Table) = {
    Scanamo.scan[RowLock](dynamoDbClient)(lockTable.name) shouldBe empty
  }
}
