package uk.ac.wellcome.platform.matcher.locking

import java.time.{Duration, Instant}
import java.util
import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model._
import com.gu.scanamo._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class DynamoRowLockDaoTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with MatcherFixtures
    with PatienceConfiguration {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(40, Seconds)),
    interval = scaled(Span(150, Millis))
  )

  import com.gu.scanamo.syntax._

  case class ThingToStore(id: String, value: String)

  private val contextId = "contextId"

  it("locks a thing") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withDynamoRowLockDao(lockTable) { dynamoRowLockDao =>
        val id = Random.nextString(32)
        whenReady(dynamoRowLockDao.lockRow(Identifier(id), contextId)) { lock =>
          lock.id shouldBe id

          val actualStored =
            Scanamo.get[RowLock](dynamoDbClient)(lockTable.name)('id -> id)
          actualStored.get match {
            case Right(storedRowLock) => storedRowLock.id shouldBe id
            case Left(failed)         => fail(s"failed to get rowLocks $failed")
          }
        }
      }
    }
  }

  it("cannot lock a locked thing") {
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

  it("can lock a locked thing that has expired") {
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
            val expiryTimeInThePast =
              Instant.now().minus(Duration.ofSeconds(1))
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

  it("unlocks a locked thing and can lock it again") {
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

  it("throws FailedLockException if there is a problem writing the lock") {
    val mockClient = mock[AmazonDynamoDB]
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withDynamoRowLockDao(mockClient, lockTable) { dynamoRowLockDao =>
        when(mockClient.putItem(any[PutItemRequest]))
          .thenThrow(new InternalServerErrorException("FAILED"))

        whenReady(
          dynamoRowLockDao.lockRow(Identifier("id"), "contextId").failed) {
          lockFailed =>
            lockFailed shouldBe a[FailedLockException]
        }

      }
    }
  }

  it(
    "throws FailedUnlockException if there is a problem unlocking - fails to read the context locks") {
    val mockClient = mock[AmazonDynamoDB]
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withDynamoRowLockDao(mockClient, lockTable) { dynamoRowLockDao =>
        when(mockClient.query(any[QueryRequest]))
          .thenThrow(new InternalServerErrorException("FAILED"))

        whenReady(dynamoRowLockDao.unlockRows("contextId").failed) {
          unlockFailed =>
            unlockFailed shouldBe a[FailedUnlockException]
        }

      }
    }
  }

  it("throws FailedUnlockException if there is a problem deleting the lock") {
    val mockClient = mock[AmazonDynamoDB]
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withDynamoRowLockDao(mockClient, lockTable) { dynamoRowLockDao =>
        when(mockClient.query(any[QueryRequest]))
          .thenReturn(aRowLockQueryResult)

        when(mockClient.deleteItem(any[DeleteItemRequest]))
          .thenThrow(new InternalServerErrorException("FAILED"))

        whenReady(dynamoRowLockDao.unlockRows("contextId").failed) {
          unlockFailed =>
            unlockFailed shouldBe a[FailedUnlockException]
        }
      }
    }
  }

  it("only one process can lock a locked thing") {
    withSpecifiedLocalDynamoDbTable(createLockTable) { lockTable =>
      withDynamoRowLockDao(lockTable) { dynamoRowLockDao =>
        val lockUnlockCycles = 5
        val parallelism = 8

        // All locks/unlocks except one will fail in each cycle
        val expectedFailedLockCount = parallelism - 1

        (1 to lockUnlockCycles).foreach(_ => {

          val thingsToLock = (1 to parallelism).map(_ => Identifier("sameId"))

          val countDownLatch = new CountDownLatch(parallelism)
          val eventualLocks = thingsToLock
            .map { thingToLock =>
              Future {
                countDownLatch.countDown()
                countDownLatch.await(10, TimeUnit.SECONDS)
              }.flatMap(_ =>
                dynamoRowLockDao
                  .lockRow(Identifier(thingToLock.id), contextId))
            }
            .map(_.recover {
              case e: FailedLockException => e
            })

          whenReady(Future.sequence(eventualLocks)) { locksAttempts =>
            locksAttempts.collect { case a: RowLock             => a }.size shouldBe 1
            locksAttempts.collect { case a: FailedLockException => a }.size shouldBe expectedFailedLockCount
          }

          whenReady(dynamoRowLockDao.unlockRows(contextId)) { _ =>
            }
        })
      }
    }
  }

  private def aRowLockQueryResult: QueryResult = {
    val rowLockAsMap: util.Map[String, AttributeValue] =
      new util.HashMap[String, AttributeValue]() {
        put("id", new AttributeValue("id"))
        put("contextId", new AttributeValue("contextId"))
        val aDate = new AttributeValue()
        aDate.setN("1")
        put("created", aDate)
        put("expires", aDate)
      }
    val results = new util.ArrayList[util.Map[String, AttributeValue]]();
    results.add(rowLockAsMap)
    val queryResult = new QueryResult()
    queryResult.setItems(results)
    queryResult
  }

}
