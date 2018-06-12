package uk.ac.wellcome.platform.matcher.lockable

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
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

        val lockableThing = locker.lock[ThingToStore](
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

        val lockableThing = locker.lock[ThingToStore](
          Identifier(thingToStore.id)) _

        // Try to gain lock get thing
        val maybeLockedThing = lockableThing(
          Scanamo.get[ThingToStore](dynamoDbClient)(tableName = thingTable.name)('id -> id)
        )

        maybeLockedThing shouldBe a[Some[_]]
        maybeLockedThing.get shouldBe a[Left[_, _]]
        maybeLockedThing.get.left.get shouldBe a[LockFailure]
        maybeLockedThing.get.left.get.t shouldBe Identifier(id)
      }
    }
  }

  it("fails when an exception is thrown getting a thing after locking") {
    withSpecifiedLocalDynamoDbTable(createLockTable _) { lockTable: LocalDynamoDb.Table =>

        implicit val dynamoLockingService = new DynamoLockingService(
          dynamoDbClient, DynamoLockingServiceConfig(lockTable.name))

        val locker = new Locker[DynamoLockingService] {
          override implicit val lockingService = dynamoLockingService
        }

        // Set thing table state
        val id = Random.nextString(32)
        val thingToStore = ThingToStore(id, "value")

        val lockableThing = locker.lock[ThingToStore](
          Identifier(thingToStore.id)) _

        // Try to gain lock get thing
        val maybeLockedThing = lockableThing(
          Scanamo.get[ThingToStore](dynamoDbClient)("this_is_not_a_real_table")('id -> id)
        )

        maybeLockedThing shouldBe a[Some[_]]
        maybeLockedThing.get shouldBe a[Left[_, _]]
        maybeLockedThing.get.left.get shouldBe a[LockFailure]
        maybeLockedThing.get.left.get.t shouldBe Identifier(id)
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

        val lockableThings = locker.lockAll[ThingToStore](identifierList) _

        // Perform locking operation
        val maybeLockedThings = lockableThings(
          Scanamo.getAll[ThingToStore](dynamoDbClient)(tableName = thingTable.name)('id -> thingList.map(_.id).toSet)
        )

        maybeLockedThings shouldBe a[Right[_, _]]
        maybeLockedThings.right.get shouldBe thingList.map(Locked(_)).toSet
      }
    }
  }

  it("fails when unable to lock a iterable of Things") {
    withSpecifiedLocalDynamoDbTable(createLockTable _) { lockTable: LocalDynamoDb.Table =>
      withSpecifiedLocalDynamoDbTable(createThingTable _) { thingTable: LocalDynamoDb.Table =>

        import Lockable._

        implicit val dynamoLockingService = new DynamoLockingService(
          dynamoDbClient, DynamoLockingServiceConfig(lockTable.name))

        val locker = new Locker[DynamoLockingService] {
          override implicit val lockingService = dynamoLockingService
        }

        val identifierList = (1 to 10).toList.map(i => Identifier(i.toString))
        val thingList = identifierList.map(id => ThingToStore(id.id, "value"))
        val idToFail :: idsToLock = identifierList.reverse

        val putList = thingList.map(Scanamo.put(dynamoDbClient)(thingTable.name)(_))

        // Check all puts successfully returned None
        putList.count(_.isEmpty) shouldBe putList.size

        // Prepare locking operation
        val lockableThings = locker.lockAll[ThingToStore](identifierList) _

        // Set lock table state
        val lockOp = idToFail.lock
        val lock = lockOp.right.get
        lock shouldBe Locked(idToFail)

        // Perform locking operation
        val maybeLockedThings = lockableThings(
          Scanamo.getAll[ThingToStore](dynamoDbClient)(tableName = thingTable.name)('id -> thingList.map(_.id).toSet)
        )

        maybeLockedThings shouldBe a[Left[_, _]]
        maybeLockedThings.left.get shouldBe a[LockFailures[_]]
        maybeLockedThings.left.get.failed shouldBe List(idToFail)
        maybeLockedThings.left.get.succeeded.toSet shouldBe idsToLock.map(Locked(_)).toSet
      }
    }
  }

  it("fails when an exception is thrown trying to get an iterable of things after locking ") {
    withSpecifiedLocalDynamoDbTable(createLockTable _) { lockTable: LocalDynamoDb.Table =>

        implicit val dynamoLockingService = new DynamoLockingService(
          dynamoDbClient, DynamoLockingServiceConfig(lockTable.name))

        val locker = new Locker[DynamoLockingService] {
          override implicit val lockingService = dynamoLockingService
        }

        val identifierList = (1 to 10).toList.map(i => Identifier(i.toString))
        val thingList = identifierList.map(id => ThingToStore(id.id, "value"))

        // Prepare locking operation
        val lockableThings = locker.lockAll[ThingToStore](identifierList) _

        // Perform locking operation
        val maybeLockedThings = lockableThings(
          Scanamo.getAll[ThingToStore](dynamoDbClient)("not_a_real_table")('id -> thingList.map(_.id).toSet)
        )

        maybeLockedThings shouldBe a[Left[_, _]]
        maybeLockedThings.left.get shouldBe a[LockFailures[_]]
        maybeLockedThings.left.get.failed shouldBe Iterable.empty[Identifier]
        maybeLockedThings.left.get.succeeded.toSet shouldBe identifierList.map(Locked(_)).toSet
      }
  }

  it("fails when unable to get any locked thing from an iterable of Things") {
    withSpecifiedLocalDynamoDbTable(createLockTable _) { lockTable: LocalDynamoDb.Table =>
      withSpecifiedLocalDynamoDbTable(createThingTable _) { thingTable: LocalDynamoDb.Table =>

        implicit val dynamoLockingService = new DynamoLockingService(
          dynamoDbClient, DynamoLockingServiceConfig(lockTable.name))

        val locker = new Locker[DynamoLockingService] {
          override implicit val lockingService = dynamoLockingService
        }

        val identifierList = (1 to 10).map(i => Identifier(i.toString))
        val thingList = identifierList.map(id => ThingToStore(id.id, "value"))

        val lockableThings = locker.lockAll[ThingToStore](identifierList) _

        // Perform locking operation
        val maybeLockedThings = lockableThings(
          Scanamo.getAll[ThingToStore](dynamoDbClient)(thingTable.name)('id -> thingList.map(_.id).toSet)
        )

        maybeLockedThings shouldBe a[Left[_, _]]
        maybeLockedThings.left.get shouldBe a[LockFailures[_]]
        maybeLockedThings.left.get.failed shouldBe Iterable.empty[Identifier]
        maybeLockedThings.left.get.succeeded.toSet shouldBe identifierList.map(Locked(_)).toSet
      }
    }
  }

  it("fails when unable to get some locked things from an iterable of Things") {
    withSpecifiedLocalDynamoDbTable(createLockTable _) { lockTable: LocalDynamoDb.Table =>
      withSpecifiedLocalDynamoDbTable(createThingTable _) { thingTable: LocalDynamoDb.Table =>

        implicit val dynamoLockingService = new DynamoLockingService(
          dynamoDbClient, DynamoLockingServiceConfig(lockTable.name))

        val locker = new Locker[DynamoLockingService] {
          override implicit val lockingService = dynamoLockingService
        }

        val identifierList = (1 to 10).map(i => Identifier(i.toString))
        val thingList = identifierList.map(id => ThingToStore(id.id, "value"))

        val putList = thingList.slice(1,5).map(Scanamo.put(dynamoDbClient)(thingTable.name)(_))

        // Check all puts successfully returned None
        putList.count(_.isEmpty) shouldBe putList.size

        val lockableThings = locker.lockAll[ThingToStore](identifierList) _

        // Perform locking operation
        val maybeLockedThings = lockableThings(
          Scanamo.getAll[ThingToStore](dynamoDbClient)(thingTable.name)('id -> thingList.map(_.id).toSet)
        )

        maybeLockedThings shouldBe a[Left[_, _]]
        maybeLockedThings.left.get shouldBe a[LockFailures[_]]
        maybeLockedThings.left.get.failed shouldBe Iterable.empty[Identifier]
        maybeLockedThings.left.get.succeeded.toSet shouldBe identifierList.map(Locked(_)).toSet
      }
    }
  }

  it("fails when getting locked things with different ids from those specified") {
    withSpecifiedLocalDynamoDbTable(createLockTable _) { lockTable: LocalDynamoDb.Table =>
      withSpecifiedLocalDynamoDbTable(createThingTable _) { thingTable: LocalDynamoDb.Table =>

        implicit val dynamoLockingService = new DynamoLockingService(
          dynamoDbClient, DynamoLockingServiceConfig(lockTable.name))

        val locker = new Locker[DynamoLockingService] {
          override implicit val lockingService = dynamoLockingService
        }

        val identifierList = (1 to 10).map(i => Identifier(i.toString))
        val badThingList = (11 to 20).map(id => ThingToStore(id.toString, "value"))

        val putList = badThingList.map(Scanamo.put(dynamoDbClient)(thingTable.name)(_))

        // Check all puts successfully returned None
        putList.count(_.isEmpty) shouldBe putList.size

        val lockableThings = locker.lockAll[ThingToStore](identifierList) _

        // Perform locking operation
        val maybeLockedThings = lockableThings(
          Scanamo.getAll[ThingToStore](dynamoDbClient)(thingTable.name)('id -> badThingList.map(_.id).toSet)
        )

        maybeLockedThings shouldBe a[Left[_, _]]
        maybeLockedThings.left.get shouldBe a[LockFailures[_]]
        maybeLockedThings.left.get.failed shouldBe Iterable.empty[Identifier]
        maybeLockedThings.left.get.succeeded.toSet shouldBe identifierList.map(Locked(_)).toSet
      }
    }
  }
}
