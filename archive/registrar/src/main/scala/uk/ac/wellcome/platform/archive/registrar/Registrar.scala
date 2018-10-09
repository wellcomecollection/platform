package uk.ac.wellcome.platform.archive.registrar

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNSAsync
import com.google.inject._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.messaging.{
  MessageStream,
  NotificationParsingFlow
}
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  NotificationMessage
}
import uk.ac.wellcome.platform.archive.common.modules.S3ClientConfig
import uk.ac.wellcome.platform.archive.registrar.factories.StorageManifestFactory
import uk.ac.wellcome.platform.archive.registrar.flows.SnsPublishFlowA
import uk.ac.wellcome.platform.archive.registrar.models._
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.s3.S3ClientFactory
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

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
      .via(NotificationParsingFlow[ArchiveComplete])
      .map(createStorageManifest)
      .collect {
        case Right((manifest, archiveComplete)) => (manifest, archiveComplete)
      }
      .mapAsync(10) {
        case (manifest, ctx) => updateStoredManifest(manifest, ctx)
      }
      .via(SnsPublishFlowA(snsConfig))
      .log("published notification")
      .filter {
        case (_, context) => context.archiveCompleteCallbackUrl.isDefined
      }

    messageStream.run("registrar", flow)
  }

  private def createStorageManifest(archiveComplete: ArchiveComplete)(
    implicit s3Client: AmazonS3) =
    StorageManifestFactory
      .create(archiveComplete.bagLocation)
      .map(manifest => (manifest, archiveComplete))

  private def updateStoredManifest(
    storageManifest: StorageManifest,
    requestContext: ArchiveComplete)(implicit ec: ExecutionContext) =
    dataStore
      .updateRecord(storageManifest.id.value)(
        ifNotExisting = (storageManifest, EmptyMetadata()))(
        ifExisting = (_, _) => (storageManifest, EmptyMetadata())
      )
      .map(_ => (storageManifest, requestContext))
}
