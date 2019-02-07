package uk.ac.wellcome.sierra_adapter.utils

import io.circe.Decoder
import org.scalatest.Assertion
import uk.ac.wellcome.fixtures._
import uk.ac.wellcome.messaging.fixtures.{MessageInfo, Messaging}
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.models.transformable.SierraTransformable._
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.{
  EmptyMetadata,
  HybridRecord,
  VersionedHybridStore
}
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait SierraAdapterHelpers extends LocalVersionedHybridStore with Messaging {
  type SierraVHS = VersionedHybridStore[SierraTransformable,
                                        EmptyMetadata,
                                        ObjectStore[SierraTransformable]]

  def withSierraVHS[R](bucket: Bucket, table: Table)(
    testWith: TestWith[SierraVHS, R]): R =
    withTypeVHS[SierraTransformable, EmptyMetadata, R](
      bucket = bucket,
      table = table) { vhs =>
      testWith(vhs)
    }

  def storeInVHS(transformable: SierraTransformable,
                 hybridStore: SierraVHS): Future[Unit] =
    hybridStore
      .updateRecord(id = transformable.sierraId.withoutCheckDigit)(
        ifNotExisting = (transformable, EmptyMetadata()))(
        ifExisting = (t, m) =>
          throw new RuntimeException(
            s"Found record ${transformable.sierraId}, but VHS should be empty")
      )
      .map(_ => ())

  def storeInVHS(transformables: List[SierraTransformable],
                 hybridStore: SierraVHS): Future[List[Unit]] =
    Future.sequence(
      transformables.map { t =>
        storeInVHS(t, hybridStore = hybridStore)
      }
    )

  def assertStored(transformable: SierraTransformable,
                   table: Table): Assertion =
    assertStored[SierraTransformable](
      table = table,
      id = transformable.sierraId.withoutCheckDigit,
      record = transformable
    )

  def assertStoredAndSent[T](t: T, id: String, topic: Topic, table: Table)(
    implicit decoder: Decoder[T]): Assertion = {
    val hybridRecord = getHybridRecord(table, id = id)

    val storedTransformable = getObjectFromS3[T](
      bucket = Bucket(hybridRecord.location.namespace),
      key = hybridRecord.location.key
    )
    storedTransformable shouldBe t

    listMessagesReceivedFromSNS(topic).map { info: MessageInfo =>
      fromJson[HybridRecord](info.message).get
    } should contain(hybridRecord)
  }

  def assertStoredAndSent(transformable: SierraTransformable,
                          topic: Topic,
                          table: Table): Assertion =
    assertStoredAndSent[SierraTransformable](
      transformable,
      id = transformable.sierraId.withoutCheckDigit,
      topic = topic,
      table = table
    )
}
