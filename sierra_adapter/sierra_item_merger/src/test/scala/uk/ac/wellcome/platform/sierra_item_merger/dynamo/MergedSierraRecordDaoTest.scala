package uk.ac.wellcome.platform.sierra_item_merger.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{ConditionalCheckFailedException, GetItemRequest, PutItemRequest}
import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.MergedSierraRecord
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.sierra_item_merger.locals.DynamoDBLocal
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.dynamo._

class MergedSierraRecordDaoTest
    extends FunSpec
    with Matchers
    with DynamoDBLocal
    with ScalaFutures
    with MockitoSugar
    with ExtendedPatience {

  val mergedSierraRecordDao =
    new MergedSierraRecordDao(dynamoDbClient, DynamoConfig(tableName))

  describe("get a merged sierra record") {
    it(
      "should return a future of merged sierra record if it exists in  DynamoDB") {
      val mergedSierraRecord = MergedSierraRecord(id = "b1111",
                                                  maybeBibData = None,
                                                  itemData = Map(),
                                                  version = 1)

      Scanamo.put(dynamoDbClient)(tableName)(mergedSierraRecord)

      whenReady(mergedSierraRecordDao.getRecord(mergedSierraRecord.id)) {
        record =>
          record shouldBe Some(mergedSierraRecord)
      }
    }

    it(
      "should return a future of None if the record does not exist in DynamoDB") {
      val mergedSierraRecord = MergedSierraRecord(id = "b1111",
                                                  maybeBibData = None,
                                                  itemData = Map(),
                                                  version = 1)

      Scanamo.put(dynamoDbClient)(tableName)(mergedSierraRecord)

      whenReady(mergedSierraRecordDao.getRecord("b88888")) { record =>
        record shouldBe None
      }
    }

    it(
      "returns a failed future with the underlying exception if the DynamoDB read fails") {
      val dynamoDbClient = mock[AmazonDynamoDB]
      val expectedException = new RuntimeException("AAAAAARGH!")
      when(dynamoDbClient.getItem(any[GetItemRequest]))
        .thenThrow(expectedException)

      val mergedSierraRecordDaoMockedDynamoClient =
        new MergedSierraRecordDao(dynamoDbClient, DynamoConfig(tableName))

      val future =
        mergedSierraRecordDaoMockedDynamoClient.getRecord("b88888")

      whenReady(future.failed) { ex =>
        ex shouldBe expectedException
      }
    }
  }

  describe("update a merged sierra record") {
    it(
      "should insert a new merged sierra record if it doesn't already exist in DynamoDB") {
      val id = "b1111"
      val mergedSierraRecord = MergedSierraRecord(id = id,
                                                  maybeBibData = None,
                                                  itemData = Map(),
                                                  version = 0)

      whenReady(mergedSierraRecordDao.updateRecord(mergedSierraRecord)) { _ =>
        Scanamo
          .get[MergedSierraRecord](dynamoDbClient)(tableName)('id -> id)
          .get shouldBe Right(
          mergedSierraRecord.copy(version = 1)
        )
      }
    }

    it(
      "should update a merged sierra record if it already exists and has a higher version") {
      val id = "b1111"
      val mergedSierraRecord = MergedSierraRecord(id = id,
        maybeBibData = None,
        itemData = Map(),
        version = 1)
      val newerMergedSierraRecord = mergedSierraRecord.copy(version = 2)

      Scanamo.put(dynamoDbClient)(tableName)(mergedSierraRecord)

      whenReady(mergedSierraRecordDao.updateRecord(newerMergedSierraRecord)) {
        _ =>
          Scanamo
            .get[MergedSierraRecord](dynamoDbClient)(tableName)('id -> id)
            .get shouldBe Right(
            newerMergedSierraRecord.copy(version = 3)
          )
      }
    }

    it(
      "should update a merged sierra record if it already exists and has a the same version") {
      val id = "b1111"
      val mergedSierraRecord = MergedSierraRecord(id = id,
                                                  maybeBibData = None,
                                                  itemData = Map(),
                                                  version = 1)

      Scanamo.put(dynamoDbClient)(tableName)(mergedSierraRecord)

      whenReady(mergedSierraRecordDao.updateRecord(mergedSierraRecord)) {
        _ =>
          Scanamo
            .get[MergedSierraRecord](dynamoDbClient)(tableName)('id -> id)
            .get shouldBe Right(
            mergedSierraRecord.copy(version = 2)
          )
      }
    }

    it(
      "should not update a merged sierra record if it already exists and has a lower version") {
      val id = "b1111"
      val mergedSierraRecord = MergedSierraRecord(id = id,
                                                  maybeBibData = None,
                                                  itemData = Map(),
                                                  version = 1)
      val newerMergedSierraRecord = MergedSierraRecord(id = id,
                                                       maybeBibData = None,
                                                       itemData = Map(),
                                                       version = 2)

      Scanamo.put(dynamoDbClient)(tableName)(newerMergedSierraRecord)

      whenReady(mergedSierraRecordDao.updateRecord(mergedSierraRecord).failed) { ex =>
        ex shouldBe a [ConditionalCheckFailedException]

        Scanamo
          .get[MergedSierraRecord](dynamoDbClient)(tableName)('id -> id)
          .get shouldBe Right(
          newerMergedSierraRecord
        )
      }
    }

    it(
      "return a failed future if the request to DynamoDb fails") {
      val dynamoDbClient = mock[AmazonDynamoDB]
      val expectedException = new RuntimeException("AAAAAARGH!")
      when(dynamoDbClient.putItem(any[PutItemRequest]))
        .thenThrow(expectedException)

      val failingDao =
        new MergedSierraRecordDao(dynamoDbClient, DynamoConfig(tableName))

      val mergedSierraRecord = MergedSierraRecord(id = "b1111",
        maybeBibData = None,
        itemData = Map(),
        version = 1)

      whenReady(failingDao.updateRecord(mergedSierraRecord).failed) { ex =>
        ex shouldBe expectedException
      }
    }
  }
}
