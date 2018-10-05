package uk.ac.wellcome.platform.archive.registrar

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNSAsync
import com.google.inject._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.MessageStream
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  NotificationMessage,
  RequestContext
}
import uk.ac.wellcome.platform.archive.common.modules.S3ClientConfig
import uk.ac.wellcome.platform.archive.registrar.flows.SnsPublishFlow
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

    val flow = Flow[NotificationMessage]
      .log("notification message")
      .map(parseNotification)
      .flatMapConcat(createStorageManifest)
      .map {
        case (manifest, ctx) => updateStoredManifest(manifest, ctx)
      }
      .via(SnsPublishFlow(snsConfig))
      .log("published notification")
      .filter {
        case (_, context) => context.callbackUrl.isDefined
      }

    messageStream.run("registrar", flow)
  }

  private def parseNotification(message: NotificationMessage) = {
    fromJson[ArchiveComplete](message.Message) match {
      case Success(bagArchiveCompleteNotification: ArchiveComplete) =>
        RequestContext(bagArchiveCompleteNotification)
      case Failure(e) =>
        throw new RuntimeException(
          s"Failed to get object location from notification: ${e.getMessage}"
        )
    }
  }

  private def createStorageManifest(requestContext: RequestContext)(
    implicit s3Client: AmazonS3,
    materializer: ActorMaterializer,
    executionContext: ExecutionContextExecutor) = {
    Source.fromFuture(
      for (manifest <- StorageManifestFactory
             .create(requestContext.bagLocation))
        yield (manifest, requestContext))
  }

  private def updateStoredManifest(storageManifest: StorageManifest,
                                   requestContext: RequestContext) = {
    dataStore.updateRecord(storageManifest.id.value)(
      ifNotExisting = (storageManifest, EmptyMetadata()))(
      ifExisting = (_, _) => (storageManifest, EmptyMetadata())
    )
    (storageManifest, requestContext)
  }
}
