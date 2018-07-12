package uk.ac.wellcome.storage.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{
  ConditionalCheckFailedException,
  GetItemRequest,
  ProvisionedThroughputExceededException,
  UpdateItemRequest
}
import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import shapeless._
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDbVersioned
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.ExecutionContext.Implicits.global

case class TestVersioned(id: String, data: String, version: Int)

class VersionedDaoTest
    extends FunSpec
    with LocalDynamoDbVersioned
    with ScalaFutures
    with ExtendedPatience
    with MockitoSugar
    with Matchers {

  def withFixtures[R](testWith: TestWith[(Table, VersionedDao), R]): R = {
    withLocalDynamoDbTable[R] { table =>
      withVersionedDao[R](table) { versionedDao =>
        testWith((table, versionedDao))
      }
    }
  }

  describe("get a record") {
    it("returns a future of a record if its in dynamo") {
      withFixtures {
        case (table, versionedDao) =>
          val testVersioned = TestVersioned(
            id = "testSource/b110101001",
            data = "whatever",
            version = 0
          )

          Scanamo.put(dynamoDbClient)(table.name)(testVersioned)

          whenReady(versionedDao.getRecord[TestVersioned](testVersioned.id)) {
            record =>
              record shouldBe Some(testVersioned)
          }
      }
    }

    it("returns a future of None if the record isn't in dynamo") {
      withFixtures {
        case (_, versionedDao) =>
          whenReady(versionedDao.getRecord[TestVersioned]("testSource/b88888")) {
            record =>
              record shouldBe None
          }
      }
    }

    it("returns a failed future with exception if dynamo read fails") {
      withLocalDynamoDbTable { table =>
        val dynamoDbClient = mock[AmazonDynamoDB]
        val expectedException = new RuntimeException("AAAAAARGH!")
        when(dynamoDbClient.getItem(any[GetItemRequest]))
          .thenThrow(expectedException)

        val testVersionedDaoMockedDynamoClient =
          new VersionedDao(
            dynamoDbClient,
            DynamoConfig(table = table.name, index = table.index)
          )

        val future =
          testVersionedDaoMockedDynamoClient.getRecord[TestVersioned](
            "testSource/b88888")

        whenReady(future.failed) { ex =>
          ex shouldBe expectedException
        }
      }
    }
  }

  describe("update a record") {
    it("inserts a new record if it doesn't already exist") {
      withFixtures {
        case (table, versionedDao) =>
          val testVersioned = TestVersioned(
            id = "testSource/b1111",
            data = "whatever",
            version = 0
          )

          val expectedTestVersioned = testVersioned.copy(version = 1)

          whenReady(versionedDao.updateRecord(testVersioned)) { _ =>
            Scanamo
              .get[TestVersioned](dynamoDbClient)(table.name)(
                'id -> testVersioned.id)
              .get shouldBe Right(expectedTestVersioned)
          }
      }
    }

    it("updates an existing record if the update has a higher version") {
      withFixtures {
        case (table, versionedDao) =>
          val testVersioned = TestVersioned(
            id = "testSource/b1111",
            data = "whatever",
            version = 0
          )

          val newerTestVersioned = testVersioned.copy(version = 2)

          Scanamo.put(dynamoDbClient)(table.name)(testVersioned)

          whenReady(
            versionedDao.updateRecord[TestVersioned](newerTestVersioned)) { _ =>
            Scanamo
              .get[TestVersioned](dynamoDbClient)(table.name)(
                'id -> testVersioned.id)
              .get shouldBe Right(
              newerTestVersioned.copy(version = 3)
            )
          }
      }
    }

    it("updates a record if it already exists and has the same version") {
      withFixtures {
        case (table, versionedDao) =>
          val testVersioned = TestVersioned(
            id = "testSource/b1111",
            data = "whatever",
            version = 1
          )

          Scanamo.put(dynamoDbClient)(table.name)(testVersioned)

          whenReady(versionedDao.updateRecord(testVersioned)) { _ =>
            Scanamo
              .get[TestVersioned](dynamoDbClient)(table.name)(
                'id -> testVersioned.id)
              .get shouldBe Right(
              testVersioned.copy(version = 2)
            )
          }
      }
    }

    it("does not update an existing record if the update has a lower version") {
      withFixtures {
        case (table, versionedDao) =>
          val testVersioned = TestVersioned(
            id = "testSource/b1111",
            data = "whatever",
            version = 1
          )

          val newerTestVersioned = TestVersioned(
            id = "testSource/b1111",
            data = "whatever",
            version = 2
          )

          Scanamo.put(dynamoDbClient)(table.name)(newerTestVersioned)

          whenReady(versionedDao.updateRecord(testVersioned).failed) { ex =>
            ex shouldBe a[DynamoNonFatalException]
            ex.getMessage should include("The conditional request failed")

            Scanamo
              .get[TestVersioned](dynamoDbClient)(table.name)(
                'id -> testVersioned.id)
              .get shouldBe Right(
              newerTestVersioned
            )
          }
      }
    }

    it("inserts an HList into dynamoDB") {
      withFixtures {
        case (table, versionedDao) =>
          val id = "111"
          val version = 3
          val testVersioned = TestVersioned(
            id = id,
            data = "whatever",
            version = version
          )

          val gen = LabelledGeneric[TestVersioned]
          val list = gen.to(testVersioned)

          val future = for {
            _ <- versionedDao.updateRecord(list)
            actualDynamoRecord <- versionedDao.getRecord[TestVersioned](id)
          } yield (actualDynamoRecord)

          whenReady(future) { actualDynamoRecord =>
            actualDynamoRecord shouldBe Some(
              testVersioned.copy(version = version + 1))
          }
      }
    }

    it(
      "does not remove fields from a record if updating only a subset of fields in a record") {
      withFixtures {
        case (table, versionedDao) =>
          val id = "111"
          val version = 3

          case class FullRecord(id: String,
                                data: String,
                                moreData: Int,
                                version: Int)

          case class PartialRecord(id: String, moreData: Int, version: Int)

          val fullRecord = FullRecord(
            id = id,
            data = "A friendly fish fry with francis and frankie in France.",
            moreData = 0,
            version = version
          )
          val newMoreData = 2

          val future = for {
            _ <- versionedDao.updateRecord(fullRecord)
            maybePartialRecord <- versionedDao.getRecord[PartialRecord](
              fullRecord.id)
            partialRecord = maybePartialRecord.get
            updatedPartialRecord = partialRecord.copy(moreData = newMoreData)
            _ <- versionedDao.updateRecord(updatedPartialRecord)
            maybeFullRecord <- versionedDao.getRecord[FullRecord](id)
          } yield maybeFullRecord

          whenReady(future) { maybeFullRecord =>
            val expectedFullRecord = fullRecord.copy(
              moreData = newMoreData,
              version = fullRecord.version + 2)
            maybeFullRecord shouldBe Some(expectedFullRecord)
          }
      }
    }

    describe("DynamoDB failures") {
      it("returns a DynamoNonFatalException if we exceed throughput limits") {
        val exceptionThrownByUpdateItem =
          new ProvisionedThroughputExceededException(
            "You tried to write to DynamoDB too quickly!"
          )
        val expectedException =
          DynamoNonFatalException(exceptionThrownByUpdateItem)

        assertRequestFailsWithCorrectException(
          exceptionThrownByUpdateItem = exceptionThrownByUpdateItem,
          expectedException = expectedException
        )
      }

      it("returns a DynamoNonFatalException if we fail the conditional update") {
        val exceptionThrownByUpdateItem = new ConditionalCheckFailedException(
          "true is not equal to false!"
        )
        val expectedException =
          DynamoNonFatalException(exceptionThrownByUpdateItem)

        assertRequestFailsWithCorrectException(
          exceptionThrownByUpdateItem = exceptionThrownByUpdateItem,
          expectedException = expectedException
        )
      }

      it("returns the raw exception if there's an unexpected error") {
        val exception = new RuntimeException("AAAAAARGH!")

        assertRequestFailsWithCorrectException(
          exceptionThrownByUpdateItem = exception,
          expectedException = exception
        )
      }

      def assertRequestFailsWithCorrectException(
        exceptionThrownByUpdateItem: Throwable,
        expectedException: Throwable) = {
        withLocalDynamoDbTable { table =>
          val mockDynamoDbClient = mock[AmazonDynamoDB]

          when(mockDynamoDbClient.updateItem(any[UpdateItemRequest]))
            .thenThrow(exceptionThrownByUpdateItem)

          val failingDao = new VersionedDao(
            dynamoDbClient = mockDynamoDbClient,
            dynamoConfig = DynamoConfig(
              table = table.name,
              index = table.index
            )
          )

          val testVersioned = TestVersioned(
            id = "testSource/b1111",
            data = "whatever",
            version = 1
          )

          whenReady(failingDao.updateRecord(testVersioned).failed) { ex =>
            ex shouldBe expectedException
          }
        }
      }
    }
  }
}
