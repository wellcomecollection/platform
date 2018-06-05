package uk.ac.wellcome.platform.goobi_reader.services

import java.io.InputStream
import java.time.Instant

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.event.S3EventNotification
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs._
import uk.ac.wellcome.platform.goobi_reader.GoobiRecordMetadata
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.VersionedHybridStore
import uk.ac.wellcome.utils.JsonUtil._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Try

class GoobiReaderWorkerService @Inject()(
  s3Client: AmazonS3,
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  versionedHybridStore: VersionedHybridStore[InputStream, GoobiRecordMetadata, ObjectStore[InputStream]]
) extends Logging {

  implicit val actorSystem: ActorSystem = system
  implicit val materialiser: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  sqsStream.foreach(
    streamName = this.getClass.getSimpleName,
    process = processMessage
  )

  def processMessage(snsNotification: NotificationMessage) = {
    debug(s"Received notification: $snsNotification")
    val processNotificationFuture = for {
      eventNotification <- Future.fromTry(Try(S3EventNotification.parseJson(snsNotification.Message)))
      _ <- Future.sequence(eventNotification.getRecords.asScala.map(updateRecord))
    } yield ()
    processNotificationFuture.recover {
      case exception: AmazonS3Exception =>
        warn(s"Failed to fetch from S3", exception)
        throw GracefulFailureException(exception)
      case exception: SdkClientException =>
        warn(s"Error parsing snsNotification with id=${snsNotification.MessageId}", exception)
        throw GracefulFailureException(exception)
      case exception: Throwable =>
        warn(s"Error processing notification id=${snsNotification.MessageId}", exception)
        throw GracefulFailureException(exception)
    }
  }

  private def updateRecord(r: S3EventNotification.S3EventNotificationRecord) = {
    val bucketName = r.getS3.getBucket.getName
    val objectKey = r.getS3.getObject.getKey
    val id = objectKey.replaceAll(".xml", "")
    val updateEventTime: Instant = Instant.ofEpochMilli(r.getEventTime.getMillis)

    val eventuallyContent = Future {
      s3Client.getObject(bucketName, objectKey).getObjectContent
    }
    eventuallyContent.flatMap( content => {
      versionedHybridStore.updateRecord(id = id)(
        ifNotExisting = (content, GoobiRecordMetadata(updateEventTime)))(
        ifExisting = (existingContent, existingMetadata) => {
          if (existingMetadata.eventTime.isBefore(updateEventTime))
            (content, GoobiRecordMetadata(updateEventTime))
          else
            (existingContent, existingMetadata)
        })
    })
  }
}
