package uk.ac.wellcome.platform.matcher.lockable

import java.time.{Duration, Instant}

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.util.TableUtils.waitUntilActive
import com.gu.scanamo._
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.syntax._
import grizzled.slf4j.Logging
import javax.inject.Inject
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb
import uk.ac.wellcome.storage.type_classes.IdGetter
import uk.ac.wellcome.storage.dynamo._

import scala.util.{Random, Success, Try}

case class RowId(value: String)
case class Identifier(value: String)
case class LockFailure(value: String)
case class RowLock(
                    id: String,
                    created: Instant,
                    expires: Instant
                  )


case class Locked[T](t: T) {
  def unlock(implicit lockable: Lockable[T]) = {
    lockable.unlock(this)
  }
}

trait Lockable[T] {
  def lock(t: T): Either[LockFailure, Locked[T]]
  def unlock(t: Locked[T]): Try[T]
}

object Lockable {
  def apply[T](implicit lockable: Lockable[T]): Lockable[T] =
    lockable

  implicit class LockableOps[A: Lockable](a: A) {
    def lock = Lockable[A].lock(a)
  }

  implicit def createLockable[T, L <: LockingService](
    implicit
      lockingService: L,
      idGetter: IdGetter[T]
  ): Lockable[T] = new Lockable[T] {
    def lock(t: T): Either[LockFailure, Locked[T]] = {
      val identifier = Identifier(idGetter.id(t))
      val lock = lockingService.lockRow(identifier)

      lock.map(_ => Locked(t))
    }
    def unlock(lockedT: Locked[T]): Try[T] = {
      val identifier = Identifier(idGetter.id(lockedT.t))
      val unlock = lockingService.unlockRow(identifier)

      unlock.map(_ => lockedT.t)
    }
  }
}

trait LockingService {
  def lockRow(id: Identifier): Either[LockFailure, RowLock]
  def unlockRow(id: Identifier): Try[Unit]
}

class DynamoLockingService @Inject()(dynamoDBClient: AmazonDynamoDB, tableName: String)
  extends LockingService with Logging {

  private val defaultDuration = Duration.ofSeconds(10)
  private val table = Table[RowLock](tableName)

  def lockRow(id: Identifier): Either[LockFailure, RowLock] = {
    val created = Instant.now()
    val expires = created.plus(defaultDuration)
    val rowLock = RowLock(id.value, created, expires)

    debug(s"Trying to create RowLock: $rowLock")

    val scanamoOps = table.given(not(attributeExists('id))).put(rowLock)
    val result = Scanamo.exec(dynamoDBClient)(scanamoOps)

    debug(s"Got $result when creating $rowLock")

    result
      .left.map(e => LockFailure(e.toString))
      .right.map(_ => rowLock)
  }

  def unlockRow(id: Identifier) = {
    val scanamoOps = table
      .delete('id -> id.value)

    val foo: DeleteItemResult = Scanamo.exec(dynamoDBClient)(scanamoOps)

    Success(())
  }
}

trait LocalLockTableDynamoDb
  extends LocalDynamoDb {
  override def createTable(table: LocalDynamoDb.Table): LocalDynamoDb.Table = {
    dynamoDbClient.createTable(
      new CreateTableRequest()
        .withTableName(table.name)
        .withKeySchema(new KeySchemaElement()
          .withAttributeName("id")
          .withKeyType(KeyType.HASH))
        .withAttributeDefinitions(
          new AttributeDefinition()
            .withAttributeName("id")
            .withAttributeType("S")
        )
        .withProvisionedThroughput(new ProvisionedThroughput()
          .withReadCapacityUnits(1L)
          .withWriteCapacityUnits(1L))
    )

    eventually {
      waitUntilActive(dynamoDbClient, table.name)
    }

    table
  }
}

case class ThingToStore(id: String, value: String)

class LockableTest extends FunSpec with Matchers with LocalLockTableDynamoDb {
  import Lockable._
  import com.gu.scanamo.syntax._

  it("creates a lock for a Thing with id") {
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

  it("cannot create a lock for a Thing if one already exists") {
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

      val secondLockOp = thingToStore.lock
      val secondLock = secondLockOp.left.get

      secondLock shouldBe a[LockFailure]

      val unlockOp = firstLock.unlock

      unlockOp shouldBe a[Try[_]]

      val thirdLockOp = thingToStore.lock
      val thirdLock = thirdLockOp.right.get

      thirdLock shouldBe Locked(thingToStore)
    }
  }
}
