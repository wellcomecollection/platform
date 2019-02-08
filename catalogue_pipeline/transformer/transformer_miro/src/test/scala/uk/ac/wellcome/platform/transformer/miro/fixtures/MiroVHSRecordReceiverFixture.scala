package uk.ac.wellcome.platform.transformer.miro.fixtures

import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.fixtures.{Messaging, SNS}
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.transformer.miro.generators.MiroRecordGenerators
import uk.ac.wellcome.platform.transformer.miro.models.MiroMetadata
import uk.ac.wellcome.platform.transformer.miro.services.MiroVHSRecordReceiver
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global

trait MiroVHSRecordReceiverFixture
    extends Messaging
    with SNS
    with MiroRecordGenerators {
  def withMiroVHSRecordReceiver[R](topic: Topic, bucket: Bucket)(
    testWith: TestWith[MiroVHSRecordReceiver, R])(
    implicit objectStore: ObjectStore[MiroRecord]): R =
    withMessageWriter[TransformedBaseWork, R](
      bucket,
      topic,
      writerSnsClient = snsClient) { messageWriter =>
      val recordReceiver = new MiroVHSRecordReceiver(
        objectStore = objectStore,
        messageWriter = messageWriter
      )

      testWith(recordReceiver)
    }

  def createMiroVHSRecordNotificationMessageWith(
    miroRecord: MiroRecord = createMiroRecord,
    bucket: Bucket,
    version: Int = 1
  ): NotificationMessage = {
    val hybridRecord = createHybridRecordWith(
      miroRecord,
      version = version,
      s3Client = s3Client,
      bucket = bucket
    )

    val miroMetadata = MiroMetadata(isClearedForCatalogueAPI = true)

    // Yes creating the JSON here manually is a bit icky, but it means this is
    // a fixed, easy-to-read output, and saves us mucking around with Circe encoders.
    createNotificationMessageWith(
      s"""
         |{
         |  "id": "${hybridRecord.id}",
         |  "location": ${toJson(hybridRecord.location).get},
         |  "version": ${hybridRecord.version},
         |  "isClearedForCatalogueAPI": ${toJson(
           miroMetadata.isClearedForCatalogueAPI).get}
         |}
       """.stripMargin
    )
  }
}
