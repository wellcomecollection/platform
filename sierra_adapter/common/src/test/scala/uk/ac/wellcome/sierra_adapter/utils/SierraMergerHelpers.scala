package uk.ac.wellcome.sierra_adapter.utils

import org.scalatest.Assertion
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.SierraTransformable._
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.{SourceMetadata, VersionedHybridStore}
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait SierraMergerHelpers extends LocalVersionedHybridStore with Messaging {
  def storeInVHS(
    transformable: SierraTransformable,
    hybridStore: VersionedHybridStore[SierraTransformable,
                                      SourceMetadata,
                                      ObjectStore[SierraTransformable]])
    : Future[Unit] =
    hybridStore.updateRecord(id = transformable.sierraId.withoutCheckDigit)(
      ifNotExisting = (transformable, SourceMetadata("sierra")))(
      ifExisting = (t, m) =>
        throw new RuntimeException(
          s"Found record ${transformable.sierraId}, but VHS should be empty")
    ).map(_ => ())

  def storeInVHS(
    transformables: List[SierraTransformable],
    hybridStore: VersionedHybridStore[SierraTransformable,
                                      SourceMetadata,
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
    table: Table): Assertion =
    assertStored[SierraTransformable](
      bucket,
      table,
      id = transformable.sierraId.withoutCheckDigit,
      record = transformable)

  def assertStoredAndSent(
    transformable: SierraTransformable,
    topic: Topic,
    bucket: Bucket,
    table: Table): Assertion = {
    val id = transformable.sierraId.withoutCheckDigit

    val hybridRecord = getHybridRecord(table, id)

    val storedTransformable = getObjectFromS3[SierraTransformable](Bucket(hybridRecord.location.namespace), hybridRecord.location.key)
    storedTransformable shouldBe transformable
    getMessages[SierraTransformable](topic) should contain(transformable)
  }
}
