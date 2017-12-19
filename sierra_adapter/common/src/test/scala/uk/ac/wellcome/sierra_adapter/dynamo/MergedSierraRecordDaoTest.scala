package uk.ac.wellcome.sierra_adapter.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{
  ConditionalCheckFailedException,
  GetItemRequest,
  PutItemRequest
}
import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.models.{MergedSierraRecord, SierraItemRecord}
import uk.ac.wellcome.platform.sierra_adapter.dynamo.MergedSierraRecordDao
import uk.ac.wellcome.sierra_adapter.locals.DynamoDBLocal
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.sierra_adapter.utils.SierraTestUtils
import uk.ac.wellcome.dynamo._

class MergedSierraRecordDaoTest extends FunSpec with SierraTestUtils {

  val mergedSierraRecordDao =
    new MergedSierraRecordDao(dynamoDbClient, Map("merger" -> DynamoConfig(tableName)))

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
        new MergedSierraRecordDao(dynamoDbClient, Map("merger" -> DynamoConfig(tableName)))

      val future =
        mergedSierraRecordDaoMockedDynamoClient.getRecord("b88888")

      whenReady(future.failed) { ex =>
        ex shouldBe expectedException
      }
    }
  }

  describe("update a merged sierra record") {
    it("inserts a new record if it doesn't already exist") {
      val id = "b1111"
      val mergedSierraRecord = MergedSierraRecord(
        id = id,
        maybeBibData = None,
        itemData = Map(
          "i111" -> SierraItemRecord(id = "i111",
                                     data = "something",
                                     modifiedDate = "2001-01-01T01:01:30Z",
                                     bibIds = List("b1111"))),
        version = 0
      )

      val expectedSierraRecord = mergedSierraRecord.copy(version = 1)

      whenReady(mergedSierraRecordDao.updateRecord(mergedSierraRecord)) { _ =>
        dynamoQueryEqualsValue('id -> id)(expectedValue = expectedSierraRecord)
      }
    }

    it("updates an existing record if the update has a higher version") {
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

    it("updates a record if it already exists and has the same version") {
      val id = "b1111"
      val mergedSierraRecord = MergedSierraRecord(id = id,
                                                  maybeBibData = None,
                                                  itemData = Map(),
                                                  version = 1)

      Scanamo.put(dynamoDbClient)(tableName)(mergedSierraRecord)

      whenReady(mergedSierraRecordDao.updateRecord(mergedSierraRecord)) { _ =>
        Scanamo
          .get[MergedSierraRecord](dynamoDbClient)(tableName)('id -> id)
          .get shouldBe Right(
          mergedSierraRecord.copy(version = 2)
        )
      }
    }

    it("does not update an existing record if the update has a lower version") {
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

      whenReady(mergedSierraRecordDao.updateRecord(mergedSierraRecord).failed) {
        ex =>
          ex shouldBe a[ConditionalCheckFailedException]

          Scanamo
            .get[MergedSierraRecord](dynamoDbClient)(tableName)('id -> id)
            .get shouldBe Right(
            newerMergedSierraRecord
          )
      }
    }

    it("returns a failed future if the request to DynamoDb fails") {
      val dynamoDbClient = mock[AmazonDynamoDB]
      val expectedException = new RuntimeException("AAAAAARGH!")
      when(dynamoDbClient.putItem(any[PutItemRequest]))
        .thenThrow(expectedException)

      val failingDao =
        new MergedSierraRecordDao(dynamoDbClient, Map("merger" -> DynamoConfig(tableName)))

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
