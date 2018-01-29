package uk.ac.wellcome.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{GetItemRequest, PutItemRequest}
import com.gu.scanamo.{DynamoFormat, Scanamo}
import com.gu.scanamo.syntax._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.{FunSpec, Matchers, Suite}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.dynamo._
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.locals.DynamoDBLocal

class SourceDataDaoTest
    extends FunSpec
    with DynamoDBLocal
    with ScalaFutures
    with MockitoSugar
    with Matchers
    with ExtendedPatience {

  override lazy val tableName: String = "source"

  val sourceDataDao =
    new SourceDataDao(dynamoDbClient, DynamoConfig(tableName))

  describe("get a record") {
    it("returns a future of a record if its in dynamo") {

      val sourceData = SourceData(
        id = "b110101001",
        reindexShard = "foo",
        reindexVersion = 0,
        ref = "http://example.com/object",
        version = 0
      )

      Scanamo.put(dynamoDbClient)(tableName)(sourceData)

      whenReady(sourceDataDao.getRecord(sourceData.id)) { record =>
        record shouldBe Some(sourceData)
      }
    }

    it("returns a future of None if the record isn't in dynamo") {
      val sourceData = SourceData(
        id = "b110101001",
        reindexShard = "foo",
        reindexVersion = 0,
        ref = "http://example.com/object",
        version = 0
      )

      Scanamo.put(dynamoDbClient)(tableName)(sourceData)

      whenReady(sourceDataDao.getRecord("b88888")) { record =>
        record shouldBe None
      }
    }

    it("returns a failed future with exception if dynamo read fails") {
      val dynamoDbClient = mock[AmazonDynamoDB]
      val expectedException = new RuntimeException("AAAAAARGH!")
      when(dynamoDbClient.getItem(any[GetItemRequest]))
        .thenThrow(expectedException)

      val sourceDataDaoMockedDynamoClient =
        new SourceDataDao(dynamoDbClient, DynamoConfig(tableName))

      val future =
        sourceDataDaoMockedDynamoClient.getRecord("b88888")

      whenReady(future.failed) { ex =>
        ex shouldBe expectedException
      }
    }
  }

  describe("update a merged sierra record") {
    it("inserts a new record if it doesn't already exist") {
      val id = "b1111"

      val sourceData = SourceData(
        id = id,
        reindexShard = "foo",
        reindexVersion = 0,
        ref = "http://example.com/object",
        version = 0
      )

      val expectedSourceData = sourceData.copy(version = 1)

      whenReady(sourceDataDao.updateRecord(sourceData)) { _ =>
        dynamoQueryEqualsValue('id -> id)(expectedValue = expectedSourceData)
      }
    }

    it("updates an existing record if the update has a higher version") {
      val id = "b1111"

      val sourceData = SourceData(
        id = id,
        reindexShard = "foo",
        reindexVersion = 0,
        ref = "http://example.com/object",
        version = 1
      )

      val newerSourceData = sourceData.copy(version = 2)

      Scanamo.put(dynamoDbClient)(tableName)(sourceData)

      whenReady(sourceDataDao.updateRecord(newerSourceData)) { _ =>
        Scanamo
          .get[SourceData](dynamoDbClient)(tableName)('id -> id)
          .get shouldBe Right(
          newerSourceData.copy(version = 3)
        )
      }
    }

    it("updates a record if it already exists and has the same version") {
      val id = "b1111"

      val sourceData = SourceData(
        id = id,
        reindexShard = "foo",
        reindexVersion = 0,
        ref = "http://example.com/object",
        version = 1
      )

      Scanamo.put(dynamoDbClient)(tableName)(sourceData)

      whenReady(sourceDataDao.updateRecord(sourceData)) { _ =>
        Scanamo
          .get[SourceData](dynamoDbClient)(tableName)('id -> id)
          .get shouldBe Right(
          sourceData.copy(version = 2)
        )
      }
    }

    it("does not update an existing record if the update has a lower version") {
      val id = "b1111"

      val sourceData = SourceData(
        id = id,
        reindexShard = "foo",
        reindexVersion = 0,
        ref = "http://example.com/object",
        version = 1
      )

      val newerSourceData = SourceData(
        id = id,
        reindexShard = "foo",
        reindexVersion = 0,
        ref = "http://example.com/object",
        version = 2
      )

      Scanamo.put(dynamoDbClient)(tableName)(newerSourceData)

      whenReady(sourceDataDao.updateRecord(sourceData).failed) { ex =>
        ex shouldBe a[ConditionalCheckFailedException]

        Scanamo
          .get[SourceData](dynamoDbClient)(tableName)('id -> id)
          .get shouldBe Right(
          newerSourceData
        )
      }
    }

    it("returns a failed future if the request to dynamo fails") {
      val dynamoDbClient = mock[AmazonDynamoDB]
      val expectedException = new RuntimeException("AAAAAARGH!")
      when(dynamoDbClient.putItem(any[PutItemRequest]))
        .thenThrow(expectedException)

      val failingDao =
        new SourceDataDao(dynamoDbClient, DynamoConfig(tableName))

      val sourceData = SourceData(
        id = "b1111",
        reindexShard = "foo",
        reindexVersion = 0,
        ref = "http://example.com/object",
        version = 1
      )

      whenReady(failingDao.updateRecord(sourceData).failed) { ex =>
        ex shouldBe expectedException
      }
    }
  }
}
