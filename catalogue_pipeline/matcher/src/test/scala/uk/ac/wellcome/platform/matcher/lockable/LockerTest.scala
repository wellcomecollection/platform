package uk.ac.wellcome.platform.matcher.lockable

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import com.gu.scanamo.error.DynamoReadError
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.fixtures.LocalLockTableDynamoDb
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb

import scala.util.Random

class LockerTest extends FunSpec with Matchers with LocalLockTableDynamoDb {

  case class ThingToStore(id: String, value: String)

  it("locks a Thing") {
    withSpecifiedLocalDynamoDbTable(createLockTable _) { lockTable: LocalDynamoDb.Table =>
      withSpecifiedLocalDynamoDbTable(createThingTable _) { thingTable: LocalDynamoDb.Table =>

        implicit val dynamoLockingService = new DynamoLockingService(
          dynamoDbClient, DynamoLockingServiceConfig(lockTable.name))

        val locker = new Locker[DynamoLockingService] {
          override implicit val lockingService = dynamoLockingService
        }

        val id = Random.nextString(32)
        val thingToStore = ThingToStore(id, "value")

        val putRecord = Scanamo.put(dynamoDbClient)(thingTable.name)(thingToStore)
        putRecord shouldBe a[None.type]

        val lockableThing = locker.lock[ThingToStore, DynamoReadError](
          Identifier(thingToStore.id)) _

        val maybeLockedThing = lockableThing(
          Scanamo.get[ThingToStore](dynamoDbClient)(tableName = thingTable.name)('id -> id)
        )

        maybeLockedThing shouldBe Some(Right(Locked(thingToStore)))
      }
    }
  }

  it("fails when unable to get a lock") {
    withSpecifiedLocalDynamoDbTable(createLockTable _) { lockTable: LocalDynamoDb.Table =>
      withSpecifiedLocalDynamoDbTable(createThingTable _) { thingTable: LocalDynamoDb.Table =>

        import Lockable._

        implicit val dynamoLockingService = new DynamoLockingService(
          dynamoDbClient, DynamoLockingServiceConfig(lockTable.name))

        val locker = new Locker[DynamoLockingService] {
          override implicit val lockingService = dynamoLockingService
        }

        // Set thing table state
        val id = Random.nextString(32)
        val thingToStore = ThingToStore(id, "value")

        val putRecord = Scanamo
          .put(dynamoDbClient)(thingTable.name)(thingToStore)
        putRecord shouldBe a[None.type]

        // Set lock table state
        val lockOp = thingToStore.lock
        val lock = lockOp.right.get
        lock shouldBe Locked(thingToStore)

        val lockableThing = locker.lock[ThingToStore, DynamoReadError](
          Identifier(thingToStore.id)) _

        // Try to gain lock get thing
        val maybeLockedThing = lockableThing(
          Scanamo.get[ThingToStore](dynamoDbClient)(tableName = thingTable.name)('id -> id)
        )

        maybeLockedThing shouldBe a[Some[_]]
        maybeLockedThing.get shouldBe a[Left[_,_]]
        maybeLockedThing.get.left.get shouldBe a[LockFailure]
        maybeLockedThing.get.left.get.t shouldBe Identifier(id)
      }
    }
  }

  it("locks a iterable of Things") {
    withSpecifiedLocalDynamoDbTable(createLockTable _) { lockTable: LocalDynamoDb.Table =>
      withSpecifiedLocalDynamoDbTable(createThingTable _) { thingTable: LocalDynamoDb.Table =>

        implicit val dynamoLockingService = new DynamoLockingService(
          dynamoDbClient, DynamoLockingServiceConfig(lockTable.name))

        val locker = new Locker[DynamoLockingService] {
          override implicit val lockingService = dynamoLockingService
        }

        val identifierList = (1 to 10).map(i => Identifier(i.toString))
        val thingList = identifierList.map(id => ThingToStore(id.id, "value"))

        val putList = thingList.map(Scanamo.put(dynamoDbClient)(thingTable.name)(_))

        // Check all puts successfully returned None
        putList.count(_.isEmpty) shouldBe putList.size

        val lockableThings = locker.lockAll[ThingToStore, DynamoReadError](identifierList) _

        val maybeLockedThings = lockableThings(
          Scanamo.getAll[ThingToStore](dynamoDbClient)(tableName = thingTable.name)('id -> thingList.map(_.id).toSet )
        )

        maybeLockedThings shouldBe a[Right[_,_]]
        maybeLockedThings.right.get shouldBe thingList.map(Locked(_)).toSet
      }
    }
  }
}
