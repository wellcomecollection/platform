package uk.ac.wellcome.platform.goobi_reader.services

import java.io.InputStream

import akka.Done
import com.amazonaws.services.s3.AmazonS3
import grizzled.slf4j.Logging
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs._
import uk.ac.wellcome.platform.goobi_reader.models.{
  GoobiRecordMetadata,
  S3Event,
  S3Record
}
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{VHSIndexEntry, VersionedHybridStore}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.typesafe.Runnable

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class GoobiReaderWorkerService(
  s3Client: AmazonS3,
  sqsStream: SQSStream[NotificationMessage],
  versionedHybridStore: VersionedHybridStore[InputStream,
                                             GoobiRecordMetadata,
                                             ObjectStore[InputStream]]
)(implicit ec: ExecutionContext)
    extends Logging
    with Runnable {

  def run(): Future[Done] =
    sqsStream.foreach(
      streamName = this.getClass.getSimpleName,
      process = processMessage
    )

  private def processMessage(snsNotification: NotificationMessage) = {
    debug(s"Received notification: $snsNotification")
    val eventuallyProcessedMessages = for {
      // AWS events are URL encoded, which means that the object key is URL encoded
      // The s3Client.putObject method doesn't want URL encoded keys, so decode it
      urlDecodedMessage <- Future.fromTry(
        Try(java.net.URLDecoder.decode(snsNotification.body, "utf-8")))
      eventNotification <- Future.fromTry(fromJson[S3Event](urlDecodedMessage))
      _ <- Future.sequence(eventNotification.Records.map(updateRecord))
    } yield ()
    eventuallyProcessedMessages.failed.foreach { e: Throwable =>
      error(
        s"Error processing message. Exception ${e.getClass.getCanonicalName} ${e.getMessage}")
    }
    eventuallyProcessedMessages
  }

  private def updateRecord(
    r: S3Record): Future[VHSIndexEntry[GoobiRecordMetadata]] = {
    val bucketName = r.s3.bucket.name
    val objectKey = r.s3.`object`.key
    val id = objectKey.replaceAll(".xml", "")
    val updateEventTime = r.eventTime

    val eventuallyContent = Future {
      debug(s"trying to retrieve object s3://$bucketName/$objectKey")
      s3Client.getObject(bucketName, objectKey).getObjectContent
    }
    eventuallyContent.flatMap(updatedContent => {
      versionedHybridStore.updateRecord(id = id)(
        ifNotExisting = (updatedContent, GoobiRecordMetadata(updateEventTime)))(
        ifExisting = (existingContent, existingMetadata) => {
          if (existingMetadata.eventTime.isBefore(updateEventTime))
            (updatedContent, GoobiRecordMetadata(updateEventTime))
          else
            (existingContent, existingMetadata)
        })
    })
  }
}
