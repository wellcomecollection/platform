package uk.ac.wellcome.platform.sierra_items_to_dynamo.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{
  ConditionalCheckFailedException,
  GetItemRequest,
  PutItemRequest
}
import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.sierra_items_to_dynamo.locals.SierraItemsToDynamoDBLocal
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord

class SierraItemRecordDaoTest
    extends FunSpec
    with Matchers
    with SierraItemsToDynamoDBLocal
    with ScalaFutures
    with MockitoSugar
    with ExtendedPatience {

  val sierraItemRecordDao =
    new SierraItemRecordDao(dynamoDbClient, DynamoConfig(tableName))

  describe("get item") {

    it("should return an item record if it exists in the database") {
      val id = "i111"
      val sierraItemRecord = SierraItemRecord(
        id = id,
        data = "{}",
        modifiedDate = "2005-01-01T00:00:00Z",
        bibIds = List())
      Scanamo.put(dynamoDbClient)(tableName)(sierraItemRecord)

      whenReady(sierraItemRecordDao.getItem(id = id)) { maybeItem =>
        maybeItem shouldBe Some(sierraItemRecord)
      }
    }

    it("should return None if the id does not exist in the database") {
      whenReady(sierraItemRecordDao.getItem(id = "i111")) { maybeItem =>
        maybeItem shouldBe None
      }
    }

    it("should fail if an exception is thrown by dynamoDbClient") {
      val dynamoDbClient = mock[AmazonDynamoDB]
      val sierraItemRecordDao =
        new SierraItemRecordDao(dynamoDbClient, dynamoConfig)

      val expectedException = new RuntimeException("BLERGH")
      when(dynamoDbClient.getItem(any[GetItemRequest]))
        .thenThrow(expectedException)

      whenReady(sierraItemRecordDao.getItem(id = "i111").failed) { ex =>
        ex shouldBe expectedException
      }
    }
  }

  describe("update item") {

    it("should insert an item") {
      val id = "i111"
      val sierraItemRecord = SierraItemRecord(
        id = id,
        data = "{}",
        modifiedDate = "2005-01-01T00:00:00Z",
        bibIds = List())

      whenReady(sierraItemRecordDao.updateItem(sierraItemRecord)) { _ =>
        Scanamo.get[SierraItemRecord](dynamoDbClient)(tableName)('id -> id) shouldBe Some(
          Right(
            sierraItemRecord.copy(version = 1)
          ))
      }
    }

    it("does not update an existing item if the update has a lower version") {
      val id = "i111"
      val sierraItemRecord = SierraItemRecord(
        id = id,
        data = "old data!",
        modifiedDate = "2005-01-01T00:00:00Z",
        bibIds = List())

      val newerSierraItemRecord = sierraItemRecord.copy(
        version = 1,
        data = "new data!"
      )

      Scanamo.put(dynamoDbClient)(tableName)(newerSierraItemRecord)

      whenReady(sierraItemRecordDao.updateItem(sierraItemRecord).failed) {
        ex =>
          ex shouldBe a[ConditionalCheckFailedException]

          Scanamo
            .get[SierraItemRecord](dynamoDbClient)(tableName)('id -> id)
            .get shouldBe Right(
            newerSierraItemRecord
          )
      }
    }

    it("should fail if an exception is thrown by dynamoDbClient") {
      val sierraItemRecord = SierraItemRecord(
        id = "i111",
        data = "{}",
        modifiedDate = "2005-01-01T00:00:00Z",
        bibIds = List())

      val dynamoDbClient = mock[AmazonDynamoDB]
      val sierraItemRecordDao =
        new SierraItemRecordDao(dynamoDbClient, dynamoConfig)

      val expectedException = new RuntimeException("BLERGH")
      when(dynamoDbClient.putItem(any[PutItemRequest]))
        .thenThrow(expectedException)

      whenReady(sierraItemRecordDao.updateItem(sierraItemRecord).failed) {
        ex =>
          ex shouldBe expectedException
      }
    }

  }

}
