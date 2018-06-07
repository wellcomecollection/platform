package uk.ac.wellcome.platform.matcher.lockable

import com.gu.scanamo._
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.fixtures.LocalLockTableDynamoDb
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

      secondLock shouldBe a[LockFailure]
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

      secondUnlock shouldBe a[UnlockFailure]
    }
  }
}
