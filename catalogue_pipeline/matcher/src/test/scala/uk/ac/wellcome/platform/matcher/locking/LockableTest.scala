package uk.ac.wellcome.platform.matcher.locking

import java.time.{Duration, Instant}
import java.util.concurrent.ForkJoinPool

import com.gu.scanamo._
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.lockable._
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb

import scala.collection.parallel.ForkJoinTaskSupport
import scala.util.Random

class LockableTest extends FunSpec with Matchers with LocalLockTableDynamoDb {

  import com.gu.scanamo.syntax._

  implicit val instantLongFormat =
    DynamoFormat.coercedXmap[Instant, Long, IllegalArgumentException](
      Instant.ofEpochSecond
    )(
      _.getEpochSecond
    )

  case class ThingToStore(id: String, value: String)

  it("locks a Thing") {
    withSpecifiedLocalDynamoDbTable(createLockTable _) { table: LocalDynamoDb.Table =>
      implicit val lockingService = new DynamoLockingService(
        dynamoDbClient, DynamoLockingServiceConfig(table.name))

      val id = Random.nextString(32)
      lockingService.lockRow(Identifier(id))

      val actualStored = Scanamo.get[RowLock](dynamoDbClient)(table.name)('id -> id)
      val rowLock = actualStored.get.right.get

      rowLock.id shouldBe id
    }
  }

  it("cannot lock a locked Thing") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { table =>
      implicit val lockingService = new DynamoLockingService(
        dynamoDbClient, DynamoLockingServiceConfig(table.name))

      val id = Random.nextString(32)
      lockingService.lockRow(Identifier(id))

      val secondLock = lockingService.lockRow(Identifier(id))
      secondLock.left.get shouldBe a[LockFailure]
    }
  }

  it("can lock a locked Thing that has expired") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { table =>
      implicit val lockingService = new DynamoLockingService(
        dynamoDbClient, DynamoLockingServiceConfig(table.name))

      val id = Random.nextString(32)
      val firstLock = lockingService.lockRow(Identifier(id))

      firstLock.right.get.id shouldBe id

      val secondLock = lockingService.lockRow(Identifier(id))
      secondLock.left.get shouldBe a[LockFailure]

      // Get the created RowLock
      val actualStored = Scanamo.get[RowLock](dynamoDbClient)(table.name)('id -> id)
      val rowLock = actualStored.get.right.get

      // Update the RowLock to be expired
      val expiryTimeInThePast = Instant.now().minus(Duration.ofSeconds(1))
      val updatedRowLock = rowLock.copy(expires = expiryTimeInThePast)
      Scanamo.put[RowLock](dynamoDbClient)(table.name)(updatedRowLock)

      // Retry locking expecting a success
      val thirdLock = lockingService.lockRow(Identifier(id))

      thirdLock.right.get.id shouldBe id
    }
  }

  it("unlocks a locked Thing") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { table =>
      implicit val lockingService = new DynamoLockingService(
        dynamoDbClient, DynamoLockingServiceConfig(table.name))

      val id = Random.nextString(32)
      val firstLock = lockingService.lockRow(Identifier(id))

      firstLock.right.get.id shouldBe id

      lockingService.unlockRow(Identifier(id))

      val actualStored = Scanamo.get[RowLock](dynamoDbClient)(table.name)('id -> id)
      actualStored shouldBe None

      val secondLock = lockingService.lockRow(Identifier(id))
      secondLock.right.get.id shouldBe id
    }
  }

  it("cannot unlock an unlocked Thing") {
    withSpecifiedLocalDynamoDbTable(createLockTable _) { table: LocalDynamoDb.Table =>
      implicit val lockingService = new DynamoLockingService(
        dynamoDbClient, DynamoLockingServiceConfig(table.name))

      val id = Random.nextString(32)
      val firstLock = lockingService.lockRow(Identifier(id))

      firstLock.right.get.id shouldBe id

      lockingService.unlockRow(Identifier(id))

      val secondUnlock = lockingService.unlockRow(Identifier(id))

      secondUnlock.left.get shouldBe a[UnlockFailure]
    }
  }

  it("only one process can lock/unlock a locked Thing") {
    withSpecifiedLocalDynamoDbTable(createLockTable _) { table: LocalDynamoDb.Table =>
      implicit val lockingService = new DynamoLockingService(
        dynamoDbClient, DynamoLockingServiceConfig(table.name))

      val lockUnlockCycles = 100
      val parallelism = 8

      // All locks/unlocks except one will fail in each cycle
      val expectedFailedLockCount = parallelism - 1

      val taskSupport = new ForkJoinTaskSupport(
        new ForkJoinPool(parallelism))

      (1 to lockUnlockCycles).map(_ => {

        // Create parallel collection for locking
        val thingsToLock = (1 to parallelism).map(_ => Identifier("same")).par

        // Set parallelism via taskSupport
        thingsToLock.tasksupport = taskSupport

        val locks = thingsToLock.map(lockingService.lockRow)
        val (lockLefts, lockRights) = locks.partition(_.isLeft)

        lockLefts.size shouldBe expectedFailedLockCount
        lockRights.size shouldBe 1

        // Create parallel collection for unlocking
        val thingsToUnlock = (1 to parallelism).map(_ => Identifier("same")).par

        // Set parallelism via taskSupport
        thingsToUnlock.tasksupport = taskSupport

        val unlocks = thingsToUnlock.map(lockingService.unlockRow)
        val (unlockLefts, unlockRights) = unlocks.partition(_.isLeft)

        unlockLefts.size shouldBe expectedFailedLockCount
        unlockRights.size shouldBe 1
      })
    }
  }
}
