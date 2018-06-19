package uk.ac.wellcome.platform.matcher.locking

import java.time.{Duration, Instant}

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.QueryRequest
import com.gu.scanamo._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.platform.matcher.lockable._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class DynamoRowLockDaoTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MatcherFixtures {

  import com.gu.scanamo.syntax._

  implicit val instantLongFormat: AnyRef with DynamoFormat[Instant] =
    DynamoFormat.coercedXmap[Instant, Long, IllegalArgumentException](
      Instant.ofEpochSecond
    )(
      _.getEpochSecond
    )

  case class ThingToStore(id: String, value: String)

  private val contextId = "contextId"

  it("locks a Thing") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withDynamoRowLockDao(lockTable) { dynamoRowLockDao =>
        val id = Random.nextString(32)
        whenReady(dynamoRowLockDao.lockRow(Identifier(id), contextId)) {
          lock =>
            lock.id shouldBe id

            val actualStored =
              Scanamo.get[RowLock](dynamoDbClient)(lockTable.name)('id -> id)
            val storedRowLock = actualStored.get.right.get
            storedRowLock.id shouldBe id
        }
      }
    }
  }

  it("cannot lock a locked Thing") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withDynamoRowLockDao(lockTable) { dynamoRowLockDao =>
        val id = Random.nextString(32)
        Scanamo.put[RowLock](dynamoDbClient)(lockTable.name)(
          RowLock(id, contextId, Instant.now, Instant.now.plusSeconds(100)))

        whenReady(dynamoRowLockDao.lockRow(Identifier(id), contextId).failed) {
          secondLockFailure =>
            secondLockFailure shouldBe a[FailedLockException]
        }
      }
    }
  }

  it("can lock a locked Thing that has expired") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withDynamoRowLockDao(lockTable) { dynamoRowLockDao =>
        val id = Random.nextString(32)
        whenReady(dynamoRowLockDao.lockRow(Identifier(id), contextId)) {
          firstLock =>
            firstLock.id shouldBe id
        }
        whenReady(dynamoRowLockDao.lockRow(Identifier(id), contextId).failed) {
          secondLockFailure =>
            secondLockFailure shouldBe a[FailedLockException]

            // Get the stored RowLock
            val actualStored =
              Scanamo.get[RowLock](dynamoDbClient)(lockTable.name)('id -> id)
            val rowLock = actualStored.get.right.get

            // Update the RowLock to be expired
            val expiryTimeInThePast = Instant.now().minus(Duration.ofSeconds(1))
            val updatedRowLock = rowLock.copy(expires = expiryTimeInThePast)
            Scanamo.put[RowLock](dynamoDbClient)(lockTable.name)(updatedRowLock)
        }
        whenReady(dynamoRowLockDao.lockRow(Identifier(id), contextId)) {
          thirdLock =>
            // Retry locking expecting a success
            thirdLock.id shouldBe id
        }
      }
    }
  }

  it("unlocks a locked Thing and can lock it again") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withDynamoRowLockDao(lockTable) { dynamoRowLockDao =>
        val id = Random.nextString(32)
        whenReady(dynamoRowLockDao.lockRow(Identifier(id), contextId)) {
          firstLock =>
            firstLock.id shouldBe id
        }

        whenReady(dynamoRowLockDao.unlockRows(contextId)) { secondLock =>
          val actualStored =
            Scanamo.get[RowLock](dynamoDbClient)(lockTable.name)('id -> id)
          actualStored shouldBe None
        }

        whenReady(dynamoRowLockDao.lockRow(Identifier(id), contextId)) {
          thirdLock =>
            thirdLock.id shouldBe id
        }
      }
    }
  }

  it("throws FailedLock exception...") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      val mockClient = mock[AmazonDynamoDB]
      withDynamoRowLockDao(mockClient, lockTable) { dynamoRowLockDao =>

        when(mockClient.query(any[QueryRequest]))
          .thenThrow(new RuntimeException("FAILED"))

        whenReady(dynamoRowLockDao.unlockRows("contextId").failed) { unlockFailed =>
          unlockFailed shouldBe a[FailedUnlockException]
        }

      }
    }
  }

  it("only one process can lock a locked Thing") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withDynamoRowLockDao(lockTable) { dynamoRowLockDao =>

        val lockUnlockCycles = 5
        val parallelism = 8

        // All locks/unlocks except one will fail in each cycle
        val expectedFailedLockCount = parallelism - 1

        (1 to lockUnlockCycles).foreach(_ => {

          val thingsToLock = (1 to parallelism).map(_ => Identifier("same"))

          val eventualLocks = thingsToLock.map { thingToLock => dynamoRowLockDao.lockRow(Identifier(thingToLock.id), contextId) }
            .map(_.recover {
              case e: FailedLockException => e
            })

          whenReady(Future.sequence(eventualLocks)) { locksAttempts =>
            locksAttempts.collect { case a: RowLock => a }.size shouldBe 1
            locksAttempts.collect { case a: FailedLockException => a }.size shouldBe expectedFailedLockCount
          }

          whenReady(dynamoRowLockDao.unlockRows(contextId)) { _ => }
        })
      }
    }
  }
}
