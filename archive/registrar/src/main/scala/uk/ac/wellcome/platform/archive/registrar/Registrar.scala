package uk.ac.wellcome.platform.archive.registrar

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.sns.scaladsl.SnsPublisher
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.s3.AmazonS3
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

import scala.concurrent.ExecutionContextExecutor
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

    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor =
      actorSystem.dispatcher

    implicit val s3Client: AmazonS3 = S3ClientFactory.create(
      region = s3ClientConfig.region,
      endpoint = s3ClientConfig.endpoint.getOrElse(""),
      accessKey = s3ClientConfig.accessKey.getOrElse(""),
      secretKey = s3ClientConfig.secretKey.getOrElse("")
    )

    val workFlow = Flow[NotificationMessage]
      .log("notification message")
      .map(parseNotification)
      .flatMapConcat(createStorageManifest)
      .map {
        case (manifest, context) => updateStoredManifest(manifest, context)
      }
      .map {
        case (manifest, context) =>
          BagRegistrationCompleteNotification(context.requestId, manifest)
      }
      .log("created notification")
      .map(serializeCompletedNotification)
      .log("notification serialised")
      .via(SnsPublisher.flow(snsConfig.topicArn))
      .log("published notification")

    messageStream.run("registrar", workFlow)
  }

  private def parseNotification(message: NotificationMessage) = {
    fromJson[BagArchiveCompleteNotification](message.Message) match {
      case Success(
          bagArchiveCompleteNotification: BagArchiveCompleteNotification) =>
        RegisterRequestContext(bagArchiveCompleteNotification)
      case Failure(e) =>
        throw new RuntimeException(
          s"Failed to get object location from notification: ${e.getMessage}"
        )
    }
  }

  private def createStorageManifest(requestContext: RegisterRequestContext)(
    implicit s3Client: AmazonS3,
    materializer: ActorMaterializer,
    executionContext: ExecutionContextExecutor) = {
    Source.fromFuture(
      for (manifest <- StorageManifestFactory
             .create(requestContext.bagLocation))
        yield (manifest, requestContext))
  }

  private def updateStoredManifest(storageManifest: StorageManifest,
                                   requestContext: RegisterRequestContext) = {
    dataStore.updateRecord(storageManifest.id.value)(
      ifNotExisting = (storageManifest, EmptyMetadata()))(
      ifExisting = (_, _) => (storageManifest, EmptyMetadata())
    )
    (storageManifest, requestContext)
  }

  private def serializeCompletedNotification(
    bagRegistrationCompleteNotification: BagRegistrationCompleteNotification) = {
    toJson(bagRegistrationCompleteNotification) match {
      case Success(json) => json
      case Failure(e)    => throw e
    }
  }
}
