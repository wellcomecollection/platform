package uk.ac.wellcome.platform.matcher.lockable

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.fixtures.LocalLockTableDynamoDb
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb

import scala.util.Random

class LockingDaoTest extends FunSpec with Matchers with LocalLockTableDynamoDb {
  case class ThingToStore(id: String, value: String)

  it("locks a Thing") {
    withSpecifiedLocalDynamoDbTable(createLockTable _) { table: LocalDynamoDb.Table =>
      implicit val lockingService = new DynamoLockingService(
        dynamoDbClient, DynamoLockingServiceConfig(table.name))

      val lockingDao = new LockingDao(dynamoDbClient, LockingDaoConfig(table.name))

      val id = Random.nextString(32)
      val thingToStore = ThingToStore(id, "value")

      val maybeLockedThing = lockingDao.lockAndGet(Identifier(thingToStore.id))

      maybeLockedThing shouldBe Locked(thingToStore)
    }
  }

}
