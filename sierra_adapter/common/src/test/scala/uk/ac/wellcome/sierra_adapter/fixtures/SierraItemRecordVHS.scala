package uk.ac.wellcome.sierra_adapter.fixtures

import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.test.fixtures.TestWith

trait SierraItemRecordVHS extends LocalVersionedHybridStore with ScalaFutures {
  def withItemRecordVHS[R](table: Table, bucket: Bucket)(
    testWith: TestWith[VersionedHybridStore[SierraItemRecord,
      EmptyMetadata,
      ObjectStore[SierraItemRecord]],
      R]): R =
    withTypeVHS[SierraItemRecord, EmptyMetadata, R](
      bucket,
      table,
      globalS3Prefix = "") { vhs =>
      testWith(vhs)
    }

  def storeSingleRecord(
    itemRecord: SierraItemRecord,
    versionedHybridStore: VersionedHybridStore[SierraItemRecord,
      EmptyMetadata,
      ObjectStore[SierraItemRecord]]
  ): Assertion = {
    val putFuture =
      versionedHybridStore.updateRecord(id = itemRecord.id.withoutCheckDigit)(
        ifNotExisting = (itemRecord, EmptyMetadata())
      )(
        ifExisting = (existingRecord, existingMetadata) =>
          throw new RuntimeException(
            s"VHS should be empty; got ($existingRecord, $existingMetadata)!")
      )

    whenReady(putFuture) { _ =>
      val getFuture =
        versionedHybridStore.getRecord(id = itemRecord.id.withoutCheckDigit)
      whenReady(getFuture) { result =>
        result.get shouldBe itemRecord
      }
    }
  }
}
