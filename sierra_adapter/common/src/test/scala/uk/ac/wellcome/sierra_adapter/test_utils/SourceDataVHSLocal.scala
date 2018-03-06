package uk.ac.wellcome.sierra_adapter.test_utils

import io.circe.{Decoder, Encoder}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{FunSpec, Suite}
import uk.ac.wellcome.models.SourceMetadata
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.transformable.SierraTransformable
import uk.ac.wellcome.storage.{HybridRecord, VersionedHybridStoreLocal}
import uk.ac.wellcome.test.utils.SQSLocal
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global

trait SourceDataVHSLocal
    extends ScalaFutures
    with Eventually
    with SQSLocal
    with VersionedHybridStoreLocal { this: Suite =>

  val hybridStore = createHybridStore[SierraTransformable]
  val queueUrl = createQueueAndReturnUrl("test_bib_merger")

  implicit val decoder = Decoder[SierraTransformable]
  implicit val encoder = Encoder[SierraTransformable]

  override lazy val tableName = "sourcedatavhslocaltable"
  override lazy val bucketName = "sourcedatavhslocalbucket"

  def assertStored(expectedRecord: SierraTransformable) = eventually {
    val future = for {
      actualRecord <- hybridStore.getRecord(expectedRecord.id)
      hybridRecord <- versionedDao.getRecord[HybridRecord](expectedRecord.id)
      sourceMetadata <- versionedDao.getRecord[SourceMetadata](
        expectedRecord.id)
    } yield (actualRecord, hybridRecord, sourceMetadata)

    whenReady(future) {
      case (
          Some(actualRecord),
          Some(hybridRecord),
          Some(sourceMetadata)
          ) => {
        actualRecord shouldBe expectedRecord
        hybridRecord.id shouldBe expectedRecord.id
        sourceMetadata.sourceName shouldBe expectedRecord.sourceName
      }
      case _ => throw new RuntimeException("Failed to retrieve some records.")
    }
  }

  def sendMessageToSQS(messageBody: String) = {
    val message = SQSMessage(
      subject = Some("Test message sent by SierraBibMergerWorkerServiceTest"),
      body = messageBody,
      topic = "topic",
      messageType = "messageType",
      timestamp = "2001-01-01T01:01:01Z"
    )
    sqsClient.sendMessage(queueUrl, toJson(message).get)
  }
}
