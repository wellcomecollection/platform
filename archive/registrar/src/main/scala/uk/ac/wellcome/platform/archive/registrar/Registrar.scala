package uk.ac.wellcome.platform.archive.registrar

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.sns.scaladsl.SnsPublisher
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.sns.AmazonSNSAsync
import com.google.inject._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.platform.archive.common.models.{
  BagArchiveCompleteNotification,
  NotificationMessage
}
import uk.ac.wellcome.platform.archive.common.modules.S3ClientConfig
import uk.ac.wellcome.platform.archive.registrar.models._
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.s3.S3ClientFactory
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}

import scala.util.{Failure, Success}

class Registrar @Inject()(
  snsClient: AmazonSNSAsync,
  snsConfig: SNSConfig,
  s3ClientConfig: S3ClientConfig,
  messageStream: MessageStream[NotificationMessage, Object],
  dataStore: VersionedHybridStore[StorageManifest,
                                  EmptyMetadata,
                                  ObjectStore[StorageManifest]],
  actorSystem: ActorSystem
) {
  def run() = {

    implicit val client = snsClient
    implicit val system = actorSystem

    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")

    implicit val materializer = ActorMaterializer()
    implicit val executionContext = actorSystem.dispatcher

    implicit val s3Client = S3ClientFactory.create(
      region = s3ClientConfig.region,
      endpoint = s3ClientConfig.endpoint.getOrElse(""),
      accessKey = s3ClientConfig.accessKey.getOrElse(""),
      secretKey = s3ClientConfig.secretKey.getOrElse("")
    )

    val workFlow = Flow[NotificationMessage]
      .log("notification message")
      .map(getBagArchiveCompleteNotification)
      .flatMapConcat(notification =>
        Source.fromFuture(
          StorageManifestFactory.create(notification.bagLocation)
      ))
      .map(storageManifest => {
        dataStore.updateRecord(storageManifest.id.value)(
          ifNotExisting = (storageManifest, EmptyMetadata())
        )(
          ifExisting = (_, _) => (storageManifest, EmptyMetadata())
        )

        storageManifest
      })
      .map(BagRegistrationCompleteNotification(_))
      .log("created notification")
      .map(toJson(_))
      .map {
        case Success(json) => json
        case Failure(e)    => throw e
      }
      .log("notification serialised")
      .via(SnsPublisher.flow(snsConfig.topicArn))
      .log("published notification")

    messageStream.run("registrar", workFlow)
  }

  private def getBagArchiveCompleteNotification(
    message: NotificationMessage) = {
    fromJson[BagArchiveCompleteNotification](message.Message) match {
      case Success(location) => location
      case Failure(e) =>
        throw new RuntimeException(
          s"Failed to get object location from notification: ${e.getMessage}"
        )
    }
  }
}
