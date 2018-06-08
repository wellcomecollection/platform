package uk.ac.wellcome.platform.matcher.lockable

import java.util.concurrent.ForkJoinPool

import com.gu.scanamo._
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.fixtures.LocalLockTableDynamoDb
import scala.collection.parallel.ForkJoinTaskSupport
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb

import scala.util.Random
import uk.ac.wellcome.storage.dynamo._


class LockableTest extends FunSpec with Matchers with LocalLockTableDynamoDb {

  import Lockable._
  import com.gu.scanamo.syntax._

  case class ThingToStore(id: String, value: String)

  it("locks a Thing") {
    withLocalDynamoDbTable { table: LocalDynamoDb.Table =>
      implicit val lockingService = new DynamoLockingService(
        dynamoDbClient, table.name)

      val id = Random.nextString(32)
      val thingToStore = ThingToStore(id, "value")
      val lockOp = thingToStore.lock

      val lock = lockOp.right.get

      lock shouldBe Locked(thingToStore)

      val actualStored = Scanamo.get[RowLock](dynamoDbClient)(table.name)('id -> id)
      val rowLock = actualStored.get.right.get

      rowLock.id shouldBe id
    }
  }

  it("cannot lock a locked Thing") {
    withLocalDynamoDbTable { table: LocalDynamoDb.Table =>
      implicit val lockingService = new DynamoLockingService(
        dynamoDbClient, table.name)

      val id = Random.nextString(32)
      val thingToStore = ThingToStore(id, "value")

      val firstLockOp = thingToStore.lock
      val firstLock = firstLockOp.right.get

      firstLock shouldBe Locked(thingToStore)

      val secondLockOp = thingToStore.lock
      val secondLock = secondLockOp.left.get

      secondLock shouldBe a[LockFailures[_]]
    }
  }

  it("unlocks a locked Thing") {
    withLocalDynamoDbTable { table: LocalDynamoDb.Table =>
      implicit val lockingService = new DynamoLockingService(
        dynamoDbClient, table.name)

      val id = Random.nextString(32)
      val thingToStore = ThingToStore(id, "value")

      val firstLockOp = thingToStore.lock
      val firstLock = firstLockOp.right.get

      firstLock shouldBe Locked(thingToStore)

      val unlockOp = firstLock.unlock
      val firstUnlock = unlockOp.right.get

      firstUnlock shouldBe thingToStore

      val secondLockOp = thingToStore.lock
      val secondLock = secondLockOp.right.get

      secondLock shouldBe Locked(thingToStore)
    }
  }

  it("cannot unlock an unlocked Thing") {
    withLocalDynamoDbTable { table: LocalDynamoDb.Table =>
      implicit val lockingService = new DynamoLockingService(
        dynamoDbClient, table.name)

      val id = Random.nextString(32)
      val thingToStore = ThingToStore(id, "value")

      val firstLockOp = thingToStore.lock
      val firstLock = firstLockOp.right.get

      firstLock shouldBe Locked(thingToStore)

      val firstUnlockOp = firstLock.unlock
      val firstUnlock = firstUnlockOp.right.get

      firstUnlock shouldBe thingToStore

      val secondUnlockOp = firstLock.unlock
      val secondUnlock = secondUnlockOp.left.get

      secondUnlock shouldBe a[UnlockFailures[_]]
    }
  }

  it("only one process can lock/unlock a locked Thing") {
    withLocalDynamoDbTable { table: LocalDynamoDb.Table =>
      implicit val lockingService = new DynamoLockingService(
        dynamoDbClient, table.name)

      val lockUnlockCycles = 100
      val parallelism = 8

      // All locks/unlocks except one will fail in each cycle
      val expectedFailedLockCount = parallelism - 1

      val id = Random.nextString(32)
      val thingToStore = ThingToStore(id, "value")

      val taskSupport = new ForkJoinTaskSupport(
        new ForkJoinPool(parallelism))

      (1 to lockUnlockCycles).map(_ => {

        // Create parallel collection for locking
        val thingsToStore = (1 to parallelism)
          .map(_ => thingToStore).par

        // Set parallelism via taskSupport
        thingsToStore.tasksupport = taskSupport

        val locks = thingsToStore.map(_.lock)
        val (lockLefts, lockRights) = locks.partition(_.isLeft)

        lockLefts.size shouldBe expectedFailedLockCount
        lockRights.size shouldBe 1

        // Create parallel collection for unlocking
        val thingsToUnlock = (1 to parallelism)
          .map(_ => lockRights.head.right.get).par

        // Set parallelism via taskSupport
        thingsToUnlock.tasksupport = taskSupport

        val unlocks = thingsToUnlock.map(_.unlock)
        val (unlockLefts, unlockRights) = unlocks.partition(_.isLeft)

        unlockLefts.size shouldBe expectedFailedLockCount
        unlockRights.size shouldBe 1
      })
    }
  }

  it("locks a list of Things") {
    withLocalDynamoDbTable { table: LocalDynamoDb.Table =>
      implicit val lockingService = new DynamoLockingService(
        dynamoDbClient, table.name)

      val lockableList = (1 to 10).map(i => ThingToStore(s"$i", "value"))
      val lockedList = lockableList.lock

      lockedList shouldBe a[Right[_, _]]

      val locks = lockedList.right.get

      locks.head shouldBe a[Locked[_]]

      lockableList.foreach((thing: ThingToStore) => {
        val actualStored = Scanamo
          .get[RowLock](dynamoDbClient)(table.name)('id -> thing.id)
        val rowLock = actualStored.get.right.get

        rowLock.id shouldBe thing.id
      })
    }
  }

  it("fails to lock a sequence of Things if any element is locked") {
    withLocalDynamoDbTable { table: LocalDynamoDb.Table =>
      implicit val lockingService = new DynamoLockingService(
        dynamoDbClient, table.name)

      val lockableList = (1 to 10).map(i => ThingToStore(s"$i", "value"))
      val singleLock = lockableList.head.lock

      singleLock shouldBe a[Right[_, _]]

      val seqLock = lockableList.lock

      seqLock shouldBe a[Left[_, _]]
    }
  }

  it("provides failed and succeeded things when failing to lock a sequence") {
    withLocalDynamoDbTable { table: LocalDynamoDb.Table =>
      implicit val lockingService = new DynamoLockingService(
        dynamoDbClient, table.name)

      val lockableList = (1 to 10).map(i => ThingToStore(s"$i", "value"))
      val singleLock = lockableList.head.lock
      singleLock shouldBe a[Right[_, _]]

      val seqLock = lockableList.lock
      seqLock shouldBe a[Left[_, _]]

      val lockFailure = seqLock.left.get

      lockFailure shouldBe a[LockFailures[_]]

      lockFailure.failed.head shouldBe a[ThingToStore]
      lockFailure.failed.size shouldBe 1
      lockFailure.failed.head shouldBe lockableList.head

      lockFailure.succeeded.head shouldBe a[Locked[_]]
      lockFailure.succeeded.size shouldBe 9
      lockFailure.succeeded shouldBe lockableList.tail.map(t => Locked(t))
    }
  }

  it("unlocks a list of locked Things") {
    withLocalDynamoDbTable { table: LocalDynamoDb.Table =>
      implicit val lockingService = new DynamoLockingService(
        dynamoDbClient, table.name)

      val lockableList = (1 to 10).map(i => ThingToStore(s"$i", "value"))
      val lockedList = lockableList.lock
      lockedList shouldBe a[Right[_, _]]

      val locks = lockedList.right.get
      locks.head shouldBe a[Locked[_]]

      lockableList.foreach((thing: ThingToStore) => {
        val actualStored = Scanamo
          .get[RowLock](dynamoDbClient)(table.name)('id -> thing.id)
        val rowLock = actualStored.get.right.get

        rowLock.id shouldBe thing.id
      })

      val unlockEither = locks.unlock
      unlockEither shouldBe a[Right[_,_]]

      val unlockedThings = unlockEither.right.get
      unlockedThings.foreach((thing: ThingToStore) => {
        val actualStored = Scanamo
          .get[RowLock](dynamoDbClient)(table.name)('id -> thing.id)

        actualStored shouldBe None
      })
    }
  }

  it("provides failed and succeeded things when failing to unlock a sequence") {
    withLocalDynamoDbTable { table: LocalDynamoDb.Table =>
      implicit val lockingService = new DynamoLockingService(
        dynamoDbClient, table.name)

      val lockableList: IndexedSeq[ThingToStore] = (1 to 10).map(i => ThingToStore(s"$i", "value"))
      val tailLock = lockableList.tail.lock
      tailLock shouldBe a[Right[_, _]]

      val locks: IndexedSeq[Locked[ThingToStore]] = tailLock.right.get
      lockableList.tail.foreach((thing: ThingToStore) => {
        val actualStored = Scanamo
          .get[RowLock](dynamoDbClient)(table.name)('id -> thing.id)
        val rowLock = actualStored.get.right.get

        rowLock.id shouldBe thing.id
      })

      val fakeUnlockedSeq: IndexedSeq[Locked[ThingToStore]] =
        locks ++ IndexedSeq(Locked[ThingToStore](lockableList.head))

      val unlockedThings = fakeUnlockedSeq.unlock

      unlockedThings shouldBe a[Left[_,_]]

      val unlockFailure = unlockedThings.left.get

      unlockFailure.failed.head shouldBe a[Locked[_]]
      unlockFailure.failed.size shouldBe 1
      unlockFailure.failed.head shouldBe Locked(lockableList.head)

      unlockFailure.succeeded.head shouldBe a[ThingToStore]
      unlockFailure.succeeded.size shouldBe 9
      unlockFailure.succeeded shouldBe lockableList.tail
    }
  }
}
