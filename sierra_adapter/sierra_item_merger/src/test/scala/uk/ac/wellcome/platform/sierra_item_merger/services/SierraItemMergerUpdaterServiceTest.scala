package uk.ac.wellcome.platform.sierra_item_merger.services

import com.gu.scanamo.DynamoFormat
import io.circe.{Decoder, Encoder}
import org.scalatest.FunSpec
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.DynamoConfig
import uk.ac.wellcome.platform.sierra_item_merger.utils.SierraItemMergerTestUtil
import uk.ac.wellcome.dynamo._
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.s3.{KeyPrefixGenerator, S3ObjectStore, SourcedKeyPrefixGenerator}
import uk.ac.wellcome.storage.{HybridRecord, VersionedHybridStore}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.models.{SourceMetadata, Sourced}

class SierraItemMergerUpdaterServiceTest
    extends FunSpec
    with MockitoSugar
    with SierraItemMergerTestUtil {

  val keyPrefixGenerator: KeyPrefixGenerator[Sourced] =
    new KeyPrefixGenerator[Sourced] {
      override def generate(obj: Sourced): String = "/"
    }

  val sierraUpdaterService = new SierraItemMergerUpdaterService(
    versionedHybridStore = new VersionedHybridStore[SierraTransformable](
      new S3ObjectStore(
        s3Client = s3Client,
        bucketName = bucketName,
        keyPrefixGenerator = keyPrefixGenerator
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
        SierraTransformable(
          sourceId = bibId,
          maybeBibData = None,
          itemData = Map(
            newItemRecord.id -> newItemRecord
          ))

      assertStored(expectedSierraTransformable)
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

    val f1 =
      hybridStore.updateRecord(oldRecord.id)(
        oldRecord
      )(identity)(SourceMetadata(oldRecord.sourceName))

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

    val f2 =
      hybridStore.updateRecord(newRecord.id)(
        newRecord
      )(identity)(SourceMetadata(newRecord.sourceName))

    whenReady(Future.sequence(List(f1, f2))) { _ =>
      whenReady(sierraUpdaterService.update(itemRecord)) { _ =>
        val expectedNewSierraTransformable =
          SierraTransformable(
            sourceId = bibIdNotExisting,
            maybeBibData = None,
            itemData = Map(itemRecord.id -> itemRecord)
          )


        assertStored(expectedNewSierraTransformable)

        val expectedUpdatedSierraTransformable = oldRecord.copy(
          itemData = Map(
            itemId -> itemRecord,
            "i888" -> otherItem
          )
        )

        assertStored(expectedUpdatedSierraTransformable)
        assertStored(newRecord)
      }
    }
  }

  it("updates an item if it receives an update with a newer date") {
    val id = "i3000003"
    val bibId = "b3000003"

    val oldRecord = SierraTransformable(
      sourceId = bibId,
      itemData = Map(
        id -> sierraItemRecord(
          id = id,
          updatedDate = "2003-03-03T03:03:03Z",
          bibIds = List(bibId)
        ))
    )

    val f1 = hybridStore.updateRecord(oldRecord.id)(
      oldRecord
    )(identity)(SourceMetadata(oldRecord.sourceName))

    whenReady(f1) { _ =>
      val newItemRecord = sierraItemRecord(
        id = id,
        updatedDate = "2014-04-04T04:04:04Z",
        bibIds = List(bibId)
      )

      whenReady(sierraUpdaterService.update(newItemRecord)) { _ =>
        val expectedSierraRecord = oldRecord.copy(
          itemData = Map(id -> newItemRecord)
        )

        assertStored(expectedSierraRecord)
      }
    }
  }

  it("unlinks an item if it is updated with an unlinked item") {
    val itemId = "i3000003"

    val bibId1 = "b9000001"
    val bibId2 = "b9000002"

    val itemRecord = sierraItemRecord(
      id = itemId,
      updatedDate = "2003-03-03T03:03:03Z",
      bibIds = List(bibId1)
    )

    val itemData = Map(
      itemRecord.id -> itemRecord
    )

    val sierraTransformable1 = SierraTransformable(
      sourceId = bibId1,
      itemData = itemData
    )

    val sierraTransformable2 = SierraTransformable(
      sourceId = bibId2
    )

    val f1 = hybridStore.updateRecord(sierraTransformable1.id)(
      sierraTransformable1
    )(_ => sierraTransformable1)(SourceMetadata(sierraTransformable1.sourceName))

    val f2 = hybridStore.updateRecord(sierraTransformable2.id)(
      sierraTransformable2
    )(identity)(SourceMetadata(sierraTransformable2.sourceName))

    val eventualUnits = Future.sequence(List(f1, f2))

    val unlinkItemRecord = itemRecord.copy(
      bibIds = List(bibId2),
      unlinkedBibIds = List(bibId1),
      modifiedDate = itemRecord.modifiedDate.plusSeconds(1)
    )

    val expectedItemData = Map(
      itemRecord.id -> itemRecord.copy(
        bibIds = List(bibId2),
        unlinkedBibIds = List(bibId1),
        modifiedDate = unlinkItemRecord.modifiedDate
      )
    )

    whenReady(eventualUnits.flatMap(_ =>
      sierraUpdaterService.update(unlinkItemRecord))) { _ =>
      val expectedSierraRecord1 = sierraTransformable1.copy(
        itemData = Map.empty
      )

      val expectedSierraRecord2 = sierraTransformable2.copy(
        itemData = expectedItemData
      )

      assertStored(expectedSierraRecord1)
      assertStored(expectedSierraRecord2)
    }
  }

  it("unlinks and updates a bib from a single call") {
    val itemId = "i3000003"

    val bibId1 = "b9000001"
    val bibId2 = "b9000002"

    val itemRecord = sierraItemRecord(
      id = itemId,
      updatedDate = "2003-03-03T03:03:03Z",
      bibIds = List(bibId1)
    )

    val itemData = Map(
      itemRecord.id -> itemRecord
    )

    val sierraTransformable1 = SierraTransformable(
      sourceId = bibId1,
      itemData = itemData
    )

    val sierraTransformable2 = SierraTransformable(
      sourceId = bibId2,
      itemData = itemData
    )

    val f1 = hybridStore.updateRecord(sierraTransformable1.id)(
      sierraTransformable1
    )(_ => sierraTransformable1)(SourceMetadata(sierraTransformable1.sourceName))

    val f2 = hybridStore.updateRecord(sierraTransformable2.id)(
      sierraTransformable2
    )(identity)(SourceMetadata(sierraTransformable2.sourceName))

    val unlinkItemRecord = itemRecord.copy(
      bibIds = List(bibId2),
      unlinkedBibIds = List(bibId1),
      modifiedDate = itemRecord.modifiedDate.plusSeconds(1)
    )

    val expectedItemData = Map(
      itemRecord.id -> unlinkItemRecord
    )

    whenReady(Future.sequence(List(f1, f2))) { _ =>

      whenReady(sierraUpdaterService.update(unlinkItemRecord)) { _ =>
        val expectedSierraRecord1 = sierraTransformable1.copy(
          itemData = Map.empty
        )

        // In this situation the item was already linked to sierraTransformable2
        // but the modified date is updated in line with the item update
        val expectedSierraRecord2 = sierraTransformable2.copy(
          itemData = expectedItemData
        )

        assertStored(expectedSierraRecord1)
      }
    }
  }

  it(
    "does not unlink an item if it receives an out of date unlink update") {
    val itemId = "i3000003"

    val bibId1 = "b9000001"
    val bibId2 = "b9000002"

    val itemRecord = sierraItemRecord(
      id = itemId,
      updatedDate = "2003-03-03T03:03:03Z",
      bibIds = List(bibId1)
    )

    val itemData = Map(
      itemRecord.id -> itemRecord
    )

    val sierraTransformable1 = SierraTransformable(
      sourceId = bibId1,
      itemData = itemData
    )

    val sierraTransformable2 = SierraTransformable(
      sourceId = bibId2
    )

    val f1 = hybridStore.updateRecord(sierraTransformable1.id)(
      sierraTransformable1
    )(_ => sierraTransformable1)(SourceMetadata(sierraTransformable1.sourceName))

    val f2 = hybridStore.updateRecord(sierraTransformable2.id)(
      sierraTransformable2
    )(identity)(SourceMetadata(sierraTransformable2.sourceName))

    val unlinkItemRecord = itemRecord.copy(
      bibIds = List(bibId2),
      unlinkedBibIds = List(bibId1),
      modifiedDate = itemRecord.modifiedDate.minusSeconds(1)
    )

    val expectedItemData = Map(
      itemRecord.id -> itemRecord.copy(
        bibIds = List(bibId2),
        unlinkedBibIds = List(bibId1),
        modifiedDate = unlinkItemRecord.modifiedDate
      )
    )

    whenReady(Future.sequence(List(f1, f2))) { _ =>

      whenReady(sierraUpdaterService.update(unlinkItemRecord)) { _ =>

        // In this situation the item will _not_ be unlinked from the original
        // record but will be linked to the new record (as this is the first
        // time we've seen the link so it is valid for that bib.
        val expectedSierraRecord1 = sierraTransformable1
        val expectedSierraRecord2 = sierraTransformable2.copy(
          itemData = expectedItemData
        )

        assertStored(expectedSierraRecord1)
        assertStored(expectedSierraRecord2)
      }
    }
  }

  it("does not update an item if it receives an update with an older date") {
    val id = "i6000006"
    val bibId = "b6000006"

    val sierraRecord = SierraTransformable(
      sourceId = bibId,
      itemData = Map(
        id -> sierraItemRecord(
          id = id,
          updatedDate = "2006-06-06T06:06:06Z",
          bibIds = List(bibId)
        ))
    )

    val f1 = hybridStore.updateRecord(sierraRecord.id)(
        sierraRecord
    )(identity)(SourceMetadata(sierraRecord.sourceName))

    val oldItemRecord = sierraItemRecord(
      id = id,
      updatedDate = "2001-01-01T01:01:01Z",
      bibIds = List(bibId)
    )

    whenReady(f1) { _ =>

      whenReady(sierraUpdaterService.update(oldItemRecord)) { _ =>

        assertStored(sierraRecord)
      }
    }
  }

  it("adds an item to the record if the bibId exists but has no itemData") {
    val bibId = "b7000007"

    val sierraRecord = SierraTransformable(
      sourceId = bibId
    )

    val f1 = hybridStore.updateRecord(sierraRecord.id)(
        sierraRecord
    )(identity)(SourceMetadata(sierraRecord.sourceName))

    val itemRecord = sierraItemRecord(
      id = "i7000007",
      updatedDate = "2007-07-07T07:07:07Z",
      bibIds = List(bibId)
    )

    whenReady(f1) { _ =>
      whenReady(sierraUpdaterService.update(itemRecord)) { _ =>
        val expectedSierraRecord = SierraTransformable(
          sourceId = bibId,
          itemData = Map(
            itemRecord.id -> itemRecord
          )
        )

        assertStored(expectedSierraRecord)
      }
    }
  }

  it("returns a failed future if putting an item fails") {

    val failingVersionedDao = mock[VersionedDao]
    val expectedException = new RuntimeException("BOOOM!")

    when(
      failingVersionedDao.getRecord[HybridRecord](
        any[String]
      )(
        any[DynamoFormat[HybridRecord]]
      ))
      .thenReturn(Future.failed(expectedException))

    val failingVersionedHybridStore =
      new VersionedHybridStore[SierraTransformable](
        sourcedObjectStore = new S3ObjectStore(
          s3Client = s3Client,
          bucketName = bucketName,
          new SourcedKeyPrefixGenerator),
        versionedDao = failingVersionedDao
      )

    val failingUpdaterService = new SierraItemMergerUpdaterService(
      failingVersionedHybridStore,
      mock[MetricsSender]
    )

    val bibId = "b242"

    val itemRecord = sierraItemRecord(
      "i000",
      "2007-07-07T07:07:07Z",
      List(bibId)
    )

    whenReady(failingUpdaterService.update(itemRecord).failed) { ex =>
      ex shouldBe expectedException
    }
  }

}
