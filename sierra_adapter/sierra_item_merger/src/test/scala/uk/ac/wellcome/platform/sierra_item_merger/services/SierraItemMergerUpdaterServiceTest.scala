package uk.ac.wellcome.platform.sierra_item_merger.services

import com.gu.scanamo.Scanamo
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.MergedSierraRecord
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.sierra_item_merger.utils.SierraItemMergerTestUtil
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.platform.sierra_adapter.dynamo.MergedSierraRecordDao
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.Future

class SierraItemMergerUpdaterServiceTest
    extends FunSpec
    with Matchers
    with SierraItemMergerTestUtil
    with MockitoSugar
    with ScalaFutures
    with ExtendedPatience {

  val sierraUpdateRService = new SierraItemMergerUpdaterService(
    mergedSierraRecordDao = new MergedSierraRecordDao(
      dynamoConfig = DynamoConfig(tableName),
      dynamoDbClient = dynamoDbClient
    ),
    mock[MetricsSender]
  )

  it(
    "should create a record in DynamoDB if it receives a item with a bib that doesn't exist") {
    val bibId = "b666"
    val newItemRecord = sierraItemRecord(
      id = "i666",
      updatedDate = "2014-04-04T04:04:04Z",
      bibIds = List(bibId)
    )

    whenReady(sierraUpdateRService.update(newItemRecord)) { _ =>
      val expectedSierraRecord =
        MergedSierraRecord(id = bibId,
                           maybeBibData = None,
                           itemData = Map(newItemRecord.id -> newItemRecord),
                           version = 1)
      dynamoQueryEqualsValue(id = bibId)(expectedValue = expectedSierraRecord)
    }
  }

  it(
    "should update multiple merged sierra records if the item contains multiple bibIds") {
    val bibIdNotExisting = "b666"
    val bibIdWithOldData = "b555"
    val bibIdWithNewerData = "b777"
    val itemId = "i666"
    val bibIds = List(bibIdNotExisting, bibIdWithOldData, bibIdWithNewerData)
    val itemRecord = sierraItemRecord(
      id = itemId,
      updatedDate = "2014-04-04T04:04:04Z",
      bibIds = bibIds
    )

    val otherItem = sierraItemRecord(id = "i888",
                                     updatedDate = "2003-03-03T03:03:03Z",
                                     bibIds = bibIds)
    val oldRecord = MergedSierraRecord(
      id = bibIdWithOldData,
      itemData = Map(itemId -> sierraItemRecord(
                       id = itemId,
                       updatedDate = "2003-03-03T03:03:03Z",
                       bibIds = bibIds
                     ),
                     "i888" -> otherItem),
      version = 1
    )
    Scanamo.put(dynamoDbClient)(tableName)(oldRecord)

    val anotherItem = sierraItemRecord(id = "i999",
                                       updatedDate = "2003-03-03T03:03:03Z",
                                       bibIds = bibIds)
    val newRecord = MergedSierraRecord(
      id = bibIdWithNewerData,
      itemData = Map(itemId -> sierraItemRecord(
                       id = itemId,
                       updatedDate = "3003-03-03T03:03:03Z",
                       bibIds = bibIds
                     ),
                     "i999" -> anotherItem),
      version = 1
    )
    Scanamo.put(dynamoDbClient)(tableName)(newRecord)

    whenReady(sierraUpdateRService.update(itemRecord)) { _ =>
      val expectedNewSierraRecord =
        MergedSierraRecord(
          id = bibIdNotExisting,
          maybeBibData = None,
          itemData = Map(itemRecord.id -> itemRecord),
          version = 1
        )

      dynamoQueryEqualsValue('id -> bibIdNotExisting)(
        expectedValue = expectedNewSierraRecord)
      val expectedUpdatedSierraRecord =
        oldRecord.copy(itemData =
                         Map(itemId -> itemRecord, "i888" -> otherItem),
                       version = 2)
      dynamoQueryEqualsValue(id = bibIdWithOldData)(
        expectedValue = expectedUpdatedSierraRecord)
      val expectedUnchangedSierraRecord = newRecord
      dynamoQueryEqualsValue(id = bibIdWithNewerData)(
        expectedValue = expectedUnchangedSierraRecord)
    }
  }

  it(
    "should update an item in DynamoDB if it receives an update with a newer date") {
    val id = "i3000003"
    val bibId = "b3000003"
    val oldRecord = MergedSierraRecord(
      id = bibId,
      itemData = Map(
        id -> sierraItemRecord(
          id = id,
          updatedDate = "2003-03-03T03:03:03Z",
          bibIds = List(bibId)
        )),
      version = 1
    )
    Scanamo.put(dynamoDbClient)(tableName)(oldRecord)

    val newItemRecord = sierraItemRecord(
      id = id,
      updatedDate = "2014-04-04T04:04:04Z",
      bibIds = List(bibId)
    )

    whenReady(sierraUpdateRService.update(newItemRecord)) { _ =>
      val expectedSierraRecord =
        oldRecord.copy(itemData = Map(id -> newItemRecord), version = 2)
      dynamoQueryEqualsValue(id = bibId)(expectedValue = expectedSierraRecord)
    }
  }

  it(
    "should not update an item in DynamoDB if it receives an update with an older date") {
    val id = "i6000006"
    val bibId = "b6000006"
    val newRecord = MergedSierraRecord(
      id = bibId,
      itemData = Map(
        id -> sierraItemRecord(
          id = id,
          updatedDate = "2006-06-06T06:06:06Z",
          bibIds = List(bibId)
        )),
      version = 1
    )
    Scanamo.put(dynamoDbClient)(tableName)(newRecord)

    val oldItemRecord = sierraItemRecord(
      id = id,
      updatedDate = "2001-01-01T01:01:01Z",
      bibIds = List(bibId)
    )

    whenReady(sierraUpdateRService.update(oldItemRecord)) { _ =>
      dynamoQueryEqualsValue(id = bibId)(expectedValue = newRecord)
    }
  }

  it(
    "should add an item to the MergedSierraRecord if the bibId exists in DynamoDB but no itemData") {
    val bibId = "b7000007"
    val newRecord = MergedSierraRecord(id = bibId, version = 1)
    Scanamo.put(dynamoDbClient)(tableName)(newRecord)

    val itemRecord = sierraItemRecord(
      id = "i7000007",
      updatedDate = "2007-07-07T07:07:07Z",
      bibIds = List(bibId)
    )

    whenReady(sierraUpdateRService.update(itemRecord)) { _ =>
      val expectedSierraRecord = MergedSierraRecord(
        id = bibId,
        itemData = Map(itemRecord.id -> itemRecord),
        version = 2
      )
      dynamoQueryEqualsValue(id = bibId)(expectedValue = expectedSierraRecord)
    }
  }

  it("should return a failed future if getting and item from the dao fails") {
    val failingDao = mock[MergedSierraRecordDao]
    val failingUpdaterService = new SierraItemMergerUpdaterService(
      mergedSierraRecordDao = failingDao,
      mock[MetricsSender]
    )
    val expectedException = new RuntimeException("BOOOM!")
    when(failingDao.getRecord(any[String]))
      .thenReturn(Future.failed(expectedException))
    val itemRecord =
      sierraItemRecord("i000", "2007-07-07T07:07:07Z", List("b242"))
    whenReady(failingUpdaterService.update(itemRecord).failed) { ex =>
      ex shouldBe expectedException
    }
  }

  it("should return a failed future if putting and item in DynamoDb fails") {
    val failingDao = mock[MergedSierraRecordDao]
    val failingUpdaterService = new SierraItemMergerUpdaterService(
      failingDao,
      mock[MetricsSender]
    )
    val expectedException = new RuntimeException("BOOOM!")
    val bibId = "b242"
    val newRecord = MergedSierraRecord(id = bibId, version = 1)
    when(failingDao.getRecord(any[String]))
      .thenReturn(Future.successful(Some(newRecord)))
    when(failingDao.updateRecord(any[MergedSierraRecord]))
      .thenReturn(Future.failed(expectedException))

    val itemRecord =
      sierraItemRecord("i000", "2007-07-07T07:07:07Z", List(bibId))

    whenReady(failingUpdaterService.update(itemRecord).failed) { ex =>
      ex shouldBe expectedException
    }
  }
}
