package uk.ac.wellcome.platform.sierra_item_merger.services

import org.scalatest.FunSpec
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.sierra_item_merger.utils.SierraItemMergerTestUtil
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.utils.JsonUtil._

import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.s3.VersionedObjectStore
import uk.ac.wellcome.storage.VersionedHybridStore

class SierraItemMergerUpdaterServiceTest
    extends FunSpec
    with SierraItemMergerTestUtil {

  val sierraUpdaterService = new SierraItemMergerUpdaterService(
    versionedHybridStore = new VersionedHybridStore(
      new VersionedObjectStore(
        s3Client = s3Client,
        bucketName = bucketName
      ),
      new VersionedDao(
        dynamoConfig = DynamoConfig(tableName),
        dynamoDbClient = dynamoDbClient
      )
    ),
    mock[MetricsSender]
  )

  it("creates a record if it receives an item with a bibId that doesn't exist") {
    val bibId = "b666"
    val newItemRecord = sierraItemRecord(
      id = "i666",
      updatedDate = "2014-04-04T04:04:04Z",
      bibIds = List(bibId)
    )

    whenReady(sierraUpdaterService.update(newItemRecord)) { _ =>
      val expectedSierraTransformable =
        SierraTransformable(sourceId = bibId,
                            maybeBibData = None,
                            itemData = Map(
                              newItemRecord.id -> newItemRecord
                            ),
                            version = 1)

      val futureRecord1 = hybridStore.getRecord[SierraTransformable](
        expectedSierraTransformable.id)
      whenReady(futureRecord1) { record =>
        record.get shouldBe expectedSierraTransformable
      }
    }
  }

  it("updates multiple merged records if the item contains multiple bibIds") {
    val bibIdNotExisting = "b666"
    val bibIdWithOldData = "b555"
    val bibIdWithNewerData = "b777"

    val itemId = "i666"

    val bibIds = List(
      bibIdNotExisting,
      bibIdWithOldData,
      bibIdWithNewerData
    )

    val itemRecord = sierraItemRecord(
      id = itemId,
      updatedDate = "2014-04-04T04:04:04Z",
      bibIds = bibIds
    )

    val otherItem = sierraItemRecord(
      id = "i888",
      updatedDate = "2003-03-03T03:03:03Z",
      bibIds = bibIds
    )

    val oldRecord = SierraTransformable(
      sourceId = bibIdWithOldData,
      itemData = Map(
        itemId -> sierraItemRecord(
          id = itemId,
          updatedDate = "2003-03-03T03:03:03Z",
          bibIds = bibIds
        ),
        "i888" -> otherItem
      )
    )

    hybridStore.updateRecord[SierraTransformable](oldRecord)

    val anotherItem = sierraItemRecord(
      id = "i999",
      updatedDate = "2003-03-03T03:03:03Z",
      bibIds = bibIds
    )

    val newRecord = SierraTransformable(
      sourceId = bibIdWithNewerData,
      itemData = Map(
        itemId -> sierraItemRecord(
          id = itemId,
          updatedDate = "3003-03-03T03:03:03Z",
          bibIds = bibIds
        ),
        "i999" -> anotherItem
      )
    )

    hybridStore.updateRecord[SierraTransformable](newRecord)

    whenReady(sierraUpdaterService.update(itemRecord)) { _ =>
      val expectedNewSierraTransformable =
        SierraTransformable(
          sourceId = bibIdNotExisting,
          maybeBibData = None,
          itemData = Map(itemRecord.id -> itemRecord),
          version = 2
        )
//
//      val futureRecord1 = hybridStore.getRecord[SierraTransformable](
//        expectedNewSierraTransformable.id)
//      whenReady(futureRecord1) { record =>
//        record.get shouldBe expectedNewSierraTransformable
//      }
//
//      val expectedUpdatedSierraTransformable = oldRecord.copy(
//        itemData = Map(
//          itemId -> itemRecord,
//          "i888" -> otherItem
//        ),
//        version = 3
//      )
//
//      val futureRecord2 = hybridStore.getRecord[SierraTransformable](
//        expectedUpdatedSierraTransformable.id)
//      whenReady(futureRecord2) { record =>
//        record.get shouldBe expectedUpdatedSierraTransformable
//      }
//
//      val expectedUnchangedSierraTranformable = newRecord
//
//      val futureRecord3 = hybridStore.getRecord[SierraTransformable](
//        expectedUnchangedSierraTranformable.id)
//      whenReady(futureRecord3) { record =>
//        record.get shouldBe expectedUnchangedSierraTranformable
//      }
    }
  }
//
//  it("updates an item if it receives an update with a newer date") {
//    val id = "i3000003"
//    val bibId = "b3000003"
//
//    val oldRecord = SierraTransformable(
//      sourceId = bibId,
//      itemData = Map(
//        id -> sierraItemRecord(
//          id = id,
//          updatedDate = "2003-03-03T03:03:03Z",
//          bibIds = List(bibId)
//        )),
//      version = 1
//    )
//
//    Scanamo.put(dynamoDbClient)(tableName)(oldRecord)
//
//    val newItemRecord = sierraItemRecord(
//      id = id,
//      updatedDate = "2014-04-04T04:04:04Z",
//      bibIds = List(bibId)
//    )
//
//    whenReady(sierraUpdaterService.update(newItemRecord)) { _ =>
//      val expectedSierraRecord = oldRecord.copy(
//        itemData = Map(
//          id -> newItemRecord
//        ),
//        version = 2
//      )
//
//      dynamoQueryEqualsValue('id -> bibId)(
//        expectedValue = expectedSierraRecord)
//    }
//  }
//
//  it(
//    "unlinks an item if it receives an update with an item specifying unlinking") {
//    val itemId = "i3000003"
//
//    val bibId1 = "b9000001"
//    val bibId2 = "b9000002"
//
//    val itemRecord = sierraItemRecord(
//      id = itemId,
//      updatedDate = "2003-03-03T03:03:03Z",
//      bibIds = List(bibId1)
//    )
//
//    val itemData = Map(
//      itemRecord.id -> itemRecord
//    )
//
//    val sierraTransformable1 = SierraTransformable(
//      sourceId = bibId1,
//      itemData = itemData,
//      version = 1
//    )
//
//    val sierraTransformable2 = SierraTransformable(
//      sourceId = bibId2,
//      itemData = Map.empty,
//      version = 1
//    )
//
//    Scanamo.put(dynamoDbClient)(tableName)(sierraTransformable1)
//    Scanamo.put(dynamoDbClient)(tableName)(sierraTransformable2)
//
//    val unlinkItemRecord = itemRecord.copy(
//      bibIds = List(bibId2),
//      unlinkedBibIds = List(bibId1),
//      modifiedDate = itemRecord.modifiedDate.plusSeconds(1)
//    )
//
//    val expectedItemData = Map(
//      itemRecord.id -> itemRecord.copy(
//        bibIds = List(bibId2),
//        unlinkedBibIds = List(bibId1),
//        modifiedDate = unlinkItemRecord.modifiedDate
//      )
//    )
//
//    whenReady(sierraUpdaterService.update(unlinkItemRecord)) { _ =>
//      val expectedSierraRecord1 = sierraTransformable1.copy(
//        itemData = Map.empty,
//        version = 2
//      )
//
//      val expectedSierraRecord2 = sierraTransformable2.copy(
//        itemData = expectedItemData,
//        version = 2
//      )
//
//      dynamoQueryEqualsValue('id -> bibId1)(
//        expectedValue = expectedSierraRecord1)
//      dynamoQueryEqualsValue('id -> bibId2)(
//        expectedValue = expectedSierraRecord2)
//    }
//  }
//
//  it("unlinks and updates a bib from a single call") {
//    val itemId = "i3000003"
//
//    val bibId1 = "b9000001"
//    val bibId2 = "b9000002"
//
//    val itemRecord = sierraItemRecord(
//      id = itemId,
//      updatedDate = "2003-03-03T03:03:03Z",
//      bibIds = List(bibId1)
//    )
//
//    val itemData = Map(
//      itemRecord.id -> itemRecord
//    )
//
//    val sierraTransformable1 = SierraTransformable(
//      sourceId = bibId1,
//      itemData = itemData,
//      version = 1
//    )
//
//    val sierraTransformable2 = SierraTransformable(
//      sourceId = bibId2,
//      itemData = itemData,
//      version = 1
//    )
//
//    Scanamo.put(dynamoDbClient)(tableName)(sierraTransformable1)
//    Scanamo.put(dynamoDbClient)(tableName)(sierraTransformable2)
//
//    val unlinkItemRecord = itemRecord.copy(
//      bibIds = List(bibId2),
//      unlinkedBibIds = List(bibId1),
//      modifiedDate = itemRecord.modifiedDate.plusSeconds(1)
//    )
//
//    val expectedItemData = Map(
//      itemRecord.id -> unlinkItemRecord
//    )
//
//    whenReady(sierraUpdaterService.update(unlinkItemRecord)) { _ =>
//      val expectedSierraRecord1 = sierraTransformable1.copy(
//        itemData = Map.empty,
//        version = 2
//      )
//
//      // In this situation the item was already linked to sierraTransformable2
//      // but the modified date is updated in line with the item update
//      val expectedSierraRecord2 = sierraTransformable2.copy(
//        itemData = expectedItemData,
//        version = 2
//      )
//
//      dynamoQueryEqualsValue('id -> bibId1)(
//        expectedValue = expectedSierraRecord1)
//    }
//  }
//
//  it(
//    "does not unlink an item from a bib if it receives an update with an item specifying unlinking which is out of date") {
//    val itemId = "i3000003"
//
//    val bibId1 = "b9000001"
//    val bibId2 = "b9000002"
//
//    val itemRecord = sierraItemRecord(
//      id = itemId,
//      updatedDate = "2003-03-03T03:03:03Z",
//      bibIds = List(bibId1)
//    )
//
//    val itemData = Map(
//      itemRecord.id -> itemRecord
//    )
//
//    val sierraTransformable1 = SierraTransformable(
//      sourceId = bibId1,
//      itemData = itemData,
//      version = 1
//    )
//
//    val sierraTransformable2 = SierraTransformable(
//      sourceId = bibId2,
//      itemData = Map.empty,
//      version = 1
//    )
//
//    Scanamo.put(dynamoDbClient)(tableName)(sierraTransformable1)
//    Scanamo.put(dynamoDbClient)(tableName)(sierraTransformable2)
//
//    val unlinkItemRecord = itemRecord.copy(
//      bibIds = List(bibId2),
//      unlinkedBibIds = List(bibId1),
//      modifiedDate = itemRecord.modifiedDate.minusSeconds(1)
//    )
//
//    val expectedItemData = Map(
//      itemRecord.id -> itemRecord.copy(
//        bibIds = List(bibId2),
//        unlinkedBibIds = List(bibId1),
//        modifiedDate = unlinkItemRecord.modifiedDate
//      )
//    )
//
//    whenReady(sierraUpdaterService.update(unlinkItemRecord)) { _ =>
//      // In this situation the item will _not_ be unlinked from the original record
//      // but will be linked to the new record (as this is the first time we've seen
//      // the link so it is valid for that bib.
//      val expectedSierraRecord1 = sierraTransformable1
//      val expectedSierraRecord2 = sierraTransformable2.copy(
//        version = 2,
//        itemData = expectedItemData
//      )
//
//      dynamoQueryEqualsValue('id -> bibId1)(
//        expectedValue = expectedSierraRecord1)
//      dynamoQueryEqualsValue('id -> bibId2)(
//        expectedValue = expectedSierraRecord2)
//    }
//  }
//
//  it("does not update an item if it receives an update with an older date") {
//    val id = "i6000006"
//    val bibId = "b6000006"
//
//    val newRecord = SierraTransformable(
//      sourceId = bibId,
//      itemData = Map(
//        id -> sierraItemRecord(
//          id = id,
//          updatedDate = "2006-06-06T06:06:06Z",
//          bibIds = List(bibId)
//        )),
//      version = 1
//    )
//
//    Scanamo.put(dynamoDbClient)(tableName)(newRecord)
//
//    val oldItemRecord = sierraItemRecord(
//      id = id,
//      updatedDate = "2001-01-01T01:01:01Z",
//      bibIds = List(bibId)
//    )
//
//    whenReady(sierraUpdaterService.update(oldItemRecord)) { _ =>
//      dynamoQueryEqualsValue('id -> bibId)(expectedValue = newRecord)
//    }
//  }
//
//  it("adds an item to the record if the bibId exists but has no itemData") {
//    val bibId = "b7000007"
//
//    val newRecord = SierraTransformable(
//      sourceId = bibId,
//      version = 1
//    )
//
//    Scanamo.put(dynamoDbClient)(tableName)(newRecord)
//
//    val itemRecord = sierraItemRecord(
//      id = "i7000007",
//      updatedDate = "2007-07-07T07:07:07Z",
//      bibIds = List(bibId)
//    )
//
//    whenReady(sierraUpdaterService.update(itemRecord)) { _ =>
//      val expectedSierraRecord = SierraTransformable(
//        sourceId = bibId,
//        itemData = Map(
//          itemRecord.id -> itemRecord
//        ),
//        version = 2
//      )
//
//      dynamoQueryEqualsValue('id -> bibId)(
//        expectedValue = expectedSierraRecord)
//    }
//  }
//
//  it("returns a failed future if getting and item from the dao fails") {
//    val failingDao = mock[SierraTransformableDao]
//
//    val failingUpdaterService = new SierraItemMergerUpdaterService(
//      sierraTransformableDao = failingDao,
//      mock[MetricsSender]
//    )
//
//    val expectedException = new RuntimeException("BOOOM!")
//
//    when(failingDao.getRecord(any[String]))
//      .thenReturn(Future.failed(expectedException))
//
//    val itemRecord = sierraItemRecord(
//      "i000",
//      "2007-07-07T07:07:07Z",
//      List("b242")
//    )
//
//    whenReady(failingUpdaterService.update(itemRecord).failed) { ex =>
//      ex shouldBe expectedException
//    }
//  }
//
//  it("returns a failed future if putting an item fails") {
//    val failingDao = mock[SierraTransformableDao]
//    val failingUpdaterService = new SierraItemMergerUpdaterService(
//      failingDao,
//      mock[MetricsSender]
//    )
//
//    val expectedException = new RuntimeException("BOOOM!")
//
//    val bibId = "b242"
//    val newRecord = SierraTransformable(sourceId = bibId, version = 1)
//
//    when(failingDao.getRecord(any[String]))
//      .thenReturn(Future.successful(Some(newRecord)))
//
//    when(failingDao.updateRecord(any[SierraTransformable]))
//      .thenReturn(Future.failed(expectedException))
//
//    val itemRecord = sierraItemRecord(
//      "i000",
//      "2007-07-07T07:07:07Z",
//      List(bibId)
//    )
//
//    whenReady(failingUpdaterService.update(itemRecord).failed) { ex =>
//      ex shouldBe expectedException
//    }
//  }
}
