package uk.ac.wellcome.sierra_adapter.utils

import io.circe.Decoder
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
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.test.fixtures._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait SierraAdapterHelpers extends LocalVersionedHybridStore with Messaging {
  type SierraVHS = VersionedHybridStore[SierraTransformable,
                                        EmptyMetadata,
                                        ObjectStore[SierraTransformable]]

  def withSierraVHS[R](bucket: Bucket, table: Table)(testWith: TestWith[SierraVHS, R]): R =
    withTypeVHS[SierraTransformable,
                EmptyMetadata,
                R](bucket = bucket, table = table) { vhs =>
      testWith(vhs)
    }

  def storeInVHS(
    transformable: SierraTransformable,
    hybridStore: SierraVHS): Future[Unit] =
    hybridStore
      .updateRecord(id = transformable.sierraId.withoutCheckDigit)(
        ifNotExisting = (transformable, EmptyMetadata()))(
        ifExisting = (t, m) =>
          throw new RuntimeException(
            s"Found record ${transformable.sierraId}, but VHS should be empty")
      )
      .map(_ => ())

  def storeInVHS(
    transformables: List[SierraTransformable],
    hybridStore: SierraVHS): Future[List[Unit]] =
    Future.sequence(
      transformables.map { t =>
        storeInVHS(t, hybridStore = hybridStore)
      }
    )

  def assertStored(transformable: SierraTransformable,
                   bucket: Bucket,
                   table: Table): Assertion =
    assertStored[SierraTransformable](
      bucket,
      table,
      id = transformable.sierraId.withoutCheckDigit,
      record = transformable)

  def assertStoredAndSent(transformable: SierraTransformable,
                          topic: Topic,
                          bucket: Bucket,
                          table: Table): Assertion =
    assertStoredAndSent[SierraTransformable](
      id = transformable.sierraId.withoutCheckDigit,
      t = transformable,
      topic = topic,
      bucket = bucket,
      table = table
    )

  def assertStoredAndSent(itemRecord: SierraItemRecord,
                          topic: Topic,
                          bucket: Bucket,
                          table: Table): Assertion =
    assertStoredAndSent[SierraItemRecord](
      id = itemRecord.id.withoutCheckDigit,
      t = itemRecord,
      topic = topic,
      bucket = bucket,
      table = table
    )

  private def assertStoredAndSent[T](
    id: String,
    t: T,
    topic: Topic,
    bucket: Bucket,
    table: Table)(implicit decoder: Decoder[T]): Assertion = {
    val hybridRecord = getHybridRecord(table, id)

    val storedTransformable = getObjectFromS3[T](
      Bucket(hybridRecord.location.namespace),
      hybridRecord.location.key)
    storedTransformable shouldBe t

    getMessages[T](topic) should contain(t)
  }
}
