package uk.ac.wellcome.platform.matcher.locking

import java.time.{Duration, Instant}

import com.gu.scanamo._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.locking._
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb

//import java.util.concurrent.ForkJoinPool
//import scala.collection.parallel.ForkJoinTaskSupport
//import scala.collection.parallel.immutable.ParSeq
//import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class DynamoRowLockDaoTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with LocalLockTableDynamoDb {

  import com.gu.scanamo.syntax._

  implicit val instantLongFormat: AnyRef with DynamoFormat[Instant] =
    DynamoFormat.coercedXmap[Instant, Long, IllegalArgumentException](
      Instant.ofEpochSecond
    )(
      _.getEpochSecond
    )

  case class ThingToStore(id: String, value: String)

  it("locks a Thing") {
    withSpecifiedLocalDynamoDbTable(createLockTable) {
      table: LocalDynamoDb.Table =>
        val dynamoRowLockDao = new DynamoRowLockDao(
          dynamoDbClient,
          DynamoLockingServiceConfig(table.name, table.index))

        val id = Random.nextString(32)
        whenReady(dynamoRowLockDao.lockRow(Identifier(id), "contextId")) {
          lock =>
            lock.id shouldBe id

            val actualStored =
              Scanamo.get[RowLock](dynamoDbClient)(table.name)('id -> id)
            val storedRowLock = actualStored.get.right.get
            storedRowLock.id shouldBe id
        }
    }
  }

  it("cannot lock a locked Thing") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      val dynamoRowLockDao = new DynamoRowLockDao(
        dynamoDbClient,
        DynamoLockingServiceConfig(lockTable.name, lockTable.index))

      val id = Random.nextString(32)
      Scanamo.put[RowLock](dynamoDbClient)(lockTable.name)(
        RowLock(id, "contextId", Instant.now, Instant.now.plusSeconds(100)))

      whenReady(dynamoRowLockDao.lockRow(Identifier(id), "contextId").failed) {
        secondLockFailure =>
          secondLockFailure shouldBe a[FailedLockException]
      }
    }
  }

  it("can lock a locked Thing that has expired") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { table =>
      val dynamoRowLockDao = new DynamoRowLockDao(
        dynamoDbClient,
        DynamoLockingServiceConfig(table.name, table.index))

      val id = Random.nextString(32)
      whenReady(dynamoRowLockDao.lockRow(Identifier(id), "contextId")) {
        firstLock =>
          firstLock.id shouldBe id
      }
      whenReady(dynamoRowLockDao.lockRow(Identifier(id), "contextId").failed) {
        secondLockFailure =>
          secondLockFailure shouldBe a[FailedLockException]

          // Get the stored RowLock
          val actualStored =
            Scanamo.get[RowLock](dynamoDbClient)(table.name)('id -> id)
          val rowLock = actualStored.get.right.get

          // Update the RowLock to be expired
          val expiryTimeInThePast = Instant.now().minus(Duration.ofSeconds(1))
          val updatedRowLock = rowLock.copy(expires = expiryTimeInThePast)
          Scanamo.put[RowLock](dynamoDbClient)(table.name)(updatedRowLock)
      }
      whenReady(dynamoRowLockDao.lockRow(Identifier(id), "contextId")) {
        thirdLock =>
          // Retry locking expecting a success
          thirdLock.id shouldBe id
      }
    }
  }

  it("unlocks a locked Thing and can lock it again") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { table =>
      val dynamoRowLockDao = new DynamoRowLockDao(
        dynamoDbClient,
        DynamoLockingServiceConfig(table.name, table.index))

      val id = Random.nextString(32)
      whenReady(dynamoRowLockDao.lockRow(Identifier(id), "contextId")) {
        firstLock =>
          firstLock.id shouldBe id
      }

      whenReady(dynamoRowLockDao.unlockRow("contextId")) { secondLock =>
        val actualStored =
          Scanamo.get[RowLock](dynamoDbClient)(table.name)('id -> id)
        actualStored shouldBe None
      }

      whenReady(dynamoRowLockDao.lockRow(Identifier(id), "contextId")) {
        thirdLock =>
          thirdLock.id shouldBe id
      }
    }
  }

//  it("only one process can lock/unlock a locked Thing") {
//    withSpecifiedLocalDynamoDbTable(createLockTable _) { table: LocalDynamoDb.Table =>
//      implicit val lockingService = new DynamoRowLockDao(
//        dynamoDbClient, DynamoLockingServiceConfig(table.name))
//
//      val lockUnlockCycles = 10
//      val parallelism = 8
//
//      // All locks/unlocks except one will fail in each cycle
////      val expectedFailedLockCount = parallelism - 1
//
//      val taskSupport = new ForkJoinTaskSupport(
//        new ForkJoinPool(parallelism))
//
//      (1 to lockUnlockCycles).map(_ => {
//
//        // Create parallel collection for locking
//        val thingsToLock = (1 to parallelism).map(_ => Identifier("same")).par
//
//        // Set parallelism via taskSupport
//        thingsToLock.tasksupport = taskSupport
//        val eventualLocks: ParSeq[Future[RowLock]] = thingsToLock.map(lockingService.lockRow)
//        whenReady(Future.sequence(eventualLocks.flatten)) {
//          eventualLocks.size shouldBe 1
//        }
//
////        // Create parallel collection for unlocking
////        val thingsToUnlock = (1 to parallelism).map(_ => Identifier("same")).par
////
////        // Set parallelism via taskSupport
////        thingsToUnlock.tasksupport = taskSupport
////
////        val unlocks = thingsToUnlock.map(lockingService.unlockRow)
////        val (unlockLefts, unlockRights) = unlocks.partition(_.isLeft)
////
////        unlockLefts.size shouldBe expectedFailedLockCount
////        unlockRights.size shouldBe 1
//      })
//    }
//  }
}
