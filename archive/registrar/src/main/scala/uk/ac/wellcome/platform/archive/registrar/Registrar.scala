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
  NotificationParsingFlow,
  SnsPublishFlow
}
import uk.ac.wellcome.platform.archive.common.models.{
  ArchiveComplete,
  NotificationMessage,
  RegistrationRequest
}
import uk.ac.wellcome.platform.archive.common.modules.S3ClientConfig
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor
import uk.ac.wellcome.platform.archive.registrar.models._
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{
  EmptyMetadata,
  HybridRecord,
  VersionedHybridStore
}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class Registrar @Inject()(
  snsConfig: SNSConfig,
  snsClient: AmazonSNSAsync,
  s3ClientConfig: S3ClientConfig,
  s3Client: AmazonS3,
  messageStream: MessageStream[NotificationMessage, Object],
  dataStore: VersionedHybridStore[BagManifest,
                                  EmptyMetadata,
                                  ObjectStore[BagManifest]],
  archiveProgressMonitor: ProgressMonitor,
)(implicit
  actorSystem: ActorSystem,
  actorMaterializer: ActorMaterializer,
  executionContext: ExecutionContext) {
  def run() = {

    implicit val client = snsClient
    implicit val system = actorSystem

    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")

    val flow = Flow[NotificationMessage]
      .via(NotificationParsingFlow[ArchiveComplete]())
      .map(RegistrationRequest(_))
      .map(registrationRequest =>
        BagManifestFactory.create(s3Client, registrationRequest.bagLocation))
      .collect { case Success(bagManifest) => bagManifest }
      .map { bagManifest =>
        updateStoredManifest(bagManifest)
      }
      .via(SnsPublishFlow(snsClient, snsConfig))
      .log("published notification")

    messageStream.run("registrar", flow)
  }

  private def updateStoredManifest(
    manifest: BagManifest): Future[(HybridRecord, EmptyMetadata)] = {
    dataStore.updateRecord(manifest.id.value)(
      ifNotExisting = (manifest, EmptyMetadata()))(
      ifExisting = (_, _) => (manifest, EmptyMetadata())
    )
  }
}
