package uk.ac.wellcome.sierra_adapter.utils

import io.circe.Encoder
import org.scalatest.Assertion
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait SierraVHSUtil extends LocalVersionedHybridStore {
  def storeInVHS(
    transformable: SierraTransformable,
    hybridStore: VersionedHybridStore[SierraTransformable,
                                      EmptyMetadata,
                                      ObjectStore[SierraTransformable]])
    : Future[Unit] =
    hybridStore.updateRecord(id = transformable.sierraId.withoutCheckDigit)(
      ifNotExisting = (transformable, EmptyMetadata()))(
      ifExisting = (t, m) =>
        throw new RuntimeException(
          s"Found record ${transformable.sierraId}, but VHS should be empty")
    )

  def storeInVHS(
    transformables: List[SierraTransformable],
    hybridStore: VersionedHybridStore[SierraTransformable,
                                      EmptyMetadata,
                                      ObjectStore[SierraTransformable]])
    : Future[List[Unit]] =
    Future.sequence(
      transformables.map { t =>
        storeInVHS(t, hybridStore = hybridStore)
      }
    )

  def assertStored(
    transformable: SierraTransformable,
    bucket: Bucket,
    table: Table)(implicit encoder: Encoder[SierraTransformable]): Assertion =
    assertStored[SierraTransformable](
      bucket,
      table,
      id = transformable.sierraId.withoutCheckDigit,
      record = transformable)
}
