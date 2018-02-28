package uk.ac.wellcome.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{
  ConditionalCheckFailedException,
  GetItemRequest,
  PutItemRequest
}
import com.gu.scanamo.syntax._
import com.gu.scanamo.{DynamoFormat, Scanamo}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.locals.DynamoDBLocal
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{Id, Versioned}

case class TestVersioned(id: String, data: String, version: Int)
    extends Versioned
    with Id

class VersionedDaoTest
    extends FunSpec
    with DynamoDBLocal[TestVersioned]
    with ScalaFutures
    with MockitoSugar
    with Matchers {

  override lazy val evidence: DynamoFormat[TestVersioned] =
    DynamoFormat[TestVersioned]

  override lazy val tableName: String = "source"

  val versionedDao =
    new VersionedDao(dynamoDbClient, DynamoConfig(tableName))

  describe("get a record") {
    it("returns a future of a record if its in dynamo") {

      val testVersioned = TestVersioned(
        id = "testSource/b110101001",
        data = "whatever",
        version = 0
      )

      Scanamo.put(dynamoDbClient)(tableName)(testVersioned)

      whenReady(versionedDao.getRecord[TestVersioned](testVersioned.id)) {
        record =>
          record shouldBe Some(testVersioned)
      }
    }

    it("returns a future of None if the record isn't in dynamo") {

      val testVersioned = TestVersioned(
        id = "testSource/b110101001",
        data = "whatever",
        version = 0
      )

      Scanamo.put(dynamoDbClient)(tableName)(testVersioned)

      whenReady(versionedDao.getRecord[TestVersioned]("testSource/b88888")) {
        record =>
          record shouldBe None
      }
    }

    it("returns a failed future with exception if dynamo read fails") {
      val dynamoDbClient = mock[AmazonDynamoDB]
      val expectedException = new RuntimeException("AAAAAARGH!")
      when(dynamoDbClient.getItem(any[GetItemRequest]))
        .thenThrow(expectedException)

      val testVersionedDaoMockedDynamoClient =
        new VersionedDao(dynamoDbClient, DynamoConfig(tableName))

      val future =
        testVersionedDaoMockedDynamoClient.getRecord[TestVersioned](
          "testSource/b88888")

      whenReady(future.failed) { ex =>
        ex shouldBe expectedException
      }
    }
  }

  describe("update a merged sierra record") {
    it("inserts a new record if it doesn't already exist") {
      val sourceId = "b1111"

      val testVersioned = TestVersioned(
        id = "testSource/b1111",
        data = "whatever",
        version = 0
      )

      val expectedTestVersioned = testVersioned.copy(version = 1)

      whenReady(versionedDao.updateRecord(testVersioned)) { _ =>
        dynamoQueryEqualsValue('id -> testVersioned.id)(
          expectedValue = expectedTestVersioned)
      }
    }

    it("updates an existing record if the update has a higher version") {
      val sourceId = "b1111"

      val testVersioned = TestVersioned(
        id = "testSource/b1111",
        data = "whatever",
        version = 0
      )

      val newerTestVersioned = testVersioned.copy(version = 2)

      Scanamo.put(dynamoDbClient)(tableName)(testVersioned)

      whenReady(versionedDao.updateRecord[TestVersioned](newerTestVersioned)) {
        _ =>
          Scanamo
            .get[TestVersioned](dynamoDbClient)(tableName)(
              'id -> testVersioned.id)
            .get shouldBe Right(
            newerTestVersioned.copy(version = 3)
          )
      }
    }

    it("updates a record if it already exists and has the same version") {

      val testVersioned = TestVersioned(
        id = "testSource/b1111",
        data = "whatever",
        version = 1
      )

      Scanamo.put(dynamoDbClient)(tableName)(testVersioned)

      whenReady(versionedDao.updateRecord(testVersioned)) { _ =>
        Scanamo
          .get[TestVersioned](dynamoDbClient)(tableName)(
            'id -> testVersioned.id)
          .get shouldBe Right(
          testVersioned.copy(version = 2)
        )
      }
    }

    it("does not update an existing record if the update has a lower version") {
      val sourceId = "b1111"

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

      Scanamo.put(dynamoDbClient)(tableName)(newerTestVersioned)

      whenReady(versionedDao.updateRecord(testVersioned).failed) { ex =>
        ex shouldBe a[ConditionalCheckFailedException]

        Scanamo
          .get[TestVersioned](dynamoDbClient)(tableName)(
            'id -> testVersioned.id)
          .get shouldBe Right(
          newerTestVersioned
        )
      }
    }

    it("returns a failed future if the request to dynamo fails") {
      val dynamoDbClient = mock[AmazonDynamoDB]
      val expectedException = new RuntimeException("AAAAAARGH!")
      when(dynamoDbClient.putItem(any[PutItemRequest]))
        .thenThrow(expectedException)

      val failingDao =
        new VersionedDao(dynamoDbClient, DynamoConfig(tableName))

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
