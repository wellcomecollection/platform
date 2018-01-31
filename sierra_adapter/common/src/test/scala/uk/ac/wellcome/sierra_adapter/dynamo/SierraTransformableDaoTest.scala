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
import uk.ac.wellcome.sierra_adapter.locals.DynamoDBLocal
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.sierra_adapter.utils.SierraTestUtils
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException

class SierraTransformableDaoTest extends FunSpec with SierraTestUtils {

  val sierraTransformableDao =
    new SierraTransformableDao(dynamoDbClient,
                               Map("merger" -> DynamoConfig(tableName)))

  describe("get a merged sierra record") {
    it("returns a future of merged sierra record if its in dynamo") {
      val sierraTransformable = SierraTransformable(sourceId = "b1111",
                                                    maybeBibData = None,
                                                    itemData = Map(),
                                                    version = 1)

      Scanamo.put(dynamoDbClient)(tableName)(sierraTransformable)

      whenReady(sierraTransformableDao.getRecord(sierraTransformable.sourceId)) {
        record =>
          record shouldBe Some(sierraTransformable)
      }
    }

    it("returns a future of None if the record isn't in dynamo") {
      val sierraTransformable = SierraTransformable(sourceId = "b1111",
                                                    maybeBibData = None,
                                                    itemData = Map(),
                                                    version = 1)

      Scanamo.put(dynamoDbClient)(tableName)(sierraTransformable)

      whenReady(sierraTransformableDao.getRecord("b88888")) { record =>
        record shouldBe None
      }
    }

    it("returns a failed future with exception if dynamo read fails") {
      val dynamoDbClient = mock[AmazonDynamoDB]
      val expectedException = new RuntimeException("AAAAAARGH!")
      when(dynamoDbClient.getItem(any[GetItemRequest]))
        .thenThrow(expectedException)

      val sierraTransformableDaoMockedDynamoClient =
        new SierraTransformableDao(dynamoDbClient,
                                   Map("merger" -> DynamoConfig(tableName)))

      val future =
        sierraTransformableDaoMockedDynamoClient.getRecord("b88888")

      whenReady(future.failed) { ex =>
        ex shouldBe expectedException
      }
    }
  }

  describe("update a merged sierra record") {
    it("inserts a new record if it doesn't already exist") {
      val id = "b1111"
      val sierraTransformable = SierraTransformable(
        sourceId = id,
        maybeBibData = None,
        itemData = Map(
          "i111" -> SierraItemRecord(id = "i111",
                                     data = "something",
                                     modifiedDate = "2001-01-01T01:01:30Z",
                                     bibIds = List("b1111"))),
        version = 0
      )

      val expectedSierraRecord = sierraTransformable.copy(version = 1)

      whenReady(sierraTransformableDao.updateRecord(sierraTransformable)) {
        _ =>
          dynamoQueryEqualsValue('sourceId -> id)(
            expectedValue = expectedSierraRecord)
      }
    }

    it("updates an existing record if the update has a higher version") {
      val id = "b1111"
      val sierraTransformable = SierraTransformable(sourceId = id,
                                                    maybeBibData = None,
                                                    itemData = Map(),
                                                    version = 1)
      val newerSierraTransformable = sierraTransformable.copy(version = 2)

      Scanamo.put(dynamoDbClient)(tableName)(sierraTransformable)

      whenReady(sierraTransformableDao.updateRecord(newerSierraTransformable)) {
        _ =>
          Scanamo
            .get[SierraTransformable](dynamoDbClient)(tableName)('sourceId -> id)
            .get shouldBe Right(
            newerSierraTransformable.copy(version = 3)
          )
      }
    }

    it("updates a record if it already exists and has the same version") {
      val id = "b1111"
      val sierraTransformable = SierraTransformable(sourceId = id,
                                                    maybeBibData = None,
                                                    itemData = Map(),
                                                    version = 1)

      Scanamo.put(dynamoDbClient)(tableName)(sierraTransformable)

      whenReady(sierraTransformableDao.updateRecord(sierraTransformable)) {
        _ =>
          Scanamo
            .get[SierraTransformable](dynamoDbClient)(tableName)('sourceId -> id)
            .get shouldBe Right(
            sierraTransformable.copy(version = 2)
          )
      }
    }

    it("does not update an existing record if the update has a lower version") {
      val id = "b1111"
      val sierraTransformable = SierraTransformable(sourceId = id,
                                                    maybeBibData = None,
                                                    itemData = Map(),
                                                    version = 1)
      val newerSierraTransformable = SierraTransformable(sourceId = id,
                                                         maybeBibData = None,
                                                         itemData = Map(),
                                                         version = 2)

      Scanamo.put(dynamoDbClient)(tableName)(newerSierraTransformable)

      whenReady(
        sierraTransformableDao.updateRecord(sierraTransformable).failed) {
        ex =>
          ex shouldBe a[ConditionalCheckFailedException]

          Scanamo
            .get[SierraTransformable](dynamoDbClient)(tableName)('sourceId -> id)
            .get shouldBe Right(
            newerSierraTransformable
          )
      }
    }

    it("returns a failed future if the request to dynamo fails") {
      val dynamoDbClient = mock[AmazonDynamoDB]
      val expectedException = new RuntimeException("AAAAAARGH!")
      when(dynamoDbClient.putItem(any[PutItemRequest]))
        .thenThrow(expectedException)

      val failingDao =
        new SierraTransformableDao(dynamoDbClient,
                                   Map("merger" -> DynamoConfig(tableName)))

      val sierraTransformable = SierraTransformable(sourceId = "b1111",
                                                    maybeBibData = None,
                                                    itemData = Map(),
                                                    version = 1)

      whenReady(failingDao.updateRecord(sierraTransformable).failed) { ex =>
        ex shouldBe expectedException
      }
    }
  }
}
