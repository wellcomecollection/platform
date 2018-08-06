package uk.ac.wellcome.sierra_adapter.utils

import org.scalatest.Assertion
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}

import scala.concurrent.Future

class SierraVHSUtil extends LocalVersionedHybridStore {
  def storeInVHS(transformable: SierraTransformable,
                 hybridStore: VersionedHybridStore[SierraTransformable,
                                                   EmptyMetadata,
                                                   ObjectStore[SierraTransformable]]): Future[Unit] =
    hybridStore.updateRecord(
      id = transformable.sierraId.withoutCheckDigit)(
      ifNotExisting = (transformable, EmptyMetadata()))(
      ifExisting = (t, m) => throw new RuntimeException(s"Found record ${transformable.sierraId}, but VHS should be empty")
    )

  def assertStored(transformable: SierraTransformable, bucket: Bucket, table: Table): Assertion =
    assertStored[SierraTransformable](
      bucket,
      table,
      id = transformable.sierraId.withoutCheckDigit,
      record = transformable)
}
