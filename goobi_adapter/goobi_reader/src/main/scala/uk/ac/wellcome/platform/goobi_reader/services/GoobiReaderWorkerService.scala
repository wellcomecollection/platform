package uk.ac.wellcome.platform.goobi_reader.services

import java.io.InputStream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.event.S3EventNotification
import com.amazonaws.services.s3.model.S3ObjectInputStream
import com.google.inject.Inject
import com.twitter.inject.Logging
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs._
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.s3.S3StreamStore
import uk.ac.wellcome.storage.vhs.{VHSConfig, VersionedHybridStore}
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.util.Try

class GoobiReaderWorkerService @Inject()(
  s3Client: AmazonS3,
  s3StreamStore: S3StreamStore,
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  vhsConfig: VHSConfig,
  versionedHybridStore: VersionedHybridStore[InputStream, S3StreamStore]
) extends Logging {

  implicit val actorSystem = system
  implicit val materialiser = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  implicit val decoderT: Decoder[SQSMessage] = deriveDecoder[SQSMessage]

  sqsStream.foreach(
    streamName = this.getClass.getSimpleName,
    process = processMessage
  )

  def processMessage(snsNotification: NotificationMessage): Future[Unit] =
    Future {
      debug(s"Received notification: $snsNotification")

      val eventNotification = Try(S3EventNotification.parseJson(snsNotification.Message)).get
      val records = eventNotification.getRecords

      records.forEach { r =>
        val bucketName = r.getS3.getBucket.getName
        val objectKey = r.getS3.getObject.getKey

        val s3Object = s3Client.getObject(bucketName, objectKey)
        val content: S3ObjectInputStream = s3Object.getObjectContent

        val id = objectKey.replaceAll(".xml", "")

        versionedHybridStore.updateRecord(id = id)(ifNotExisting = content)(identity)()
      }
    }
}
