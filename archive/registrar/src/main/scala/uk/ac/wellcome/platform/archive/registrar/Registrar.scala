package uk.ac.wellcome.platform.archive.registrar

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.stream.scaladsl.Flow
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import com.google.inject._
import com.google.inject.name.Named
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.archive.common.flows.FoldEitherFlow
import uk.ac.wellcome.platform.archive.common.messaging.{MessageStream, NotificationParsingFlow, SnsPublishFlow}
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.models.{ArchiveComplete, NotificationMessage}
import uk.ac.wellcome.platform.archive.common.modules.S3ClientConfig
import uk.ac.wellcome.platform.archive.common.progress.models.{Progress, ProgressEvent, ProgressUpdate}
import uk.ac.wellcome.platform.archive.registrar.factories.StorageManifestFactory
import uk.ac.wellcome.platform.archive.registrar.flows.NotifyDDSFlow
import uk.ac.wellcome.platform.archive.registrar.models._
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class Registrar @Inject()(
  snsClient: AmazonSNS,
  @Named("ddsSnsConfig") ddsSnsConfig: SNSConfig,
  @Named("progressSnsConfig") progressSnsConfig: SNSConfig,
  s3Client: AmazonS3,
  s3ClientConfig: S3ClientConfig,
  messageStream: MessageStream[NotificationMessage, Unit],
  dataStore: VersionedHybridStore[StorageManifest,
                                  EmptyMetadata,
                                  ObjectStore[StorageManifest]],
  actorSystem: ActorSystem
) extends Logging{
  def run() = {

    implicit val snsclient = snsClient
    implicit val system = actorSystem
    implicit val s3client = s3Client

    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")

    val decider: Supervision.Decider = {
      case e => {
        error("Stream failure", e)
        Supervision.Resume
      }
    }

    implicit val materializer: ActorMaterializer = ActorMaterializer(
      ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider)
    )
    implicit val executionContext: ExecutionContextExecutor =
      actorSystem.dispatcher

    val flow = Flow[NotificationMessage]
      .log("notification message")
      .via(NotificationParsingFlow[ArchiveComplete])
      .map(createStorageManifest)
      .log("created storage manifest")
      .via(FoldEitherFlow[ArchiveError[ArchiveComplete], (StorageManifest, ArchiveComplete), Unit](ifLeft = notifyFailure(snsclient, progressSnsConfig))(ifRight = fhgsdv()))

    messageStream.run("registrar", flow)
  }

  def notifyFailure(snsClient:AmazonSNS, snsConfig:SNSConfig) =
    Flow[ArchiveError[ArchiveComplete]]
      .map(error => ProgressUpdate(error.t.archiveRequestId, List(ProgressEvent(error.toString)), Progress.Failed))
    .via(SnsPublishFlow(snsClient, snsConfig, Some("registrar_failed"))).map(_ => ())

  def fhgsdv()(implicit snsClient: AmazonSNS, ec: ExecutionContext)
    : Flow[(StorageManifest, ArchiveComplete), Unit, NotUsed]
    = Flow[(StorageManifest, ArchiveComplete)]
    .mapAsync(10) {
      case (manifest, ctx) => updateStoredManifest(manifest, ctx)
    }
    .via(NotifyDDSFlow(ddsSnsConfig))
    .map(archiveComplete => ProgressUpdate(archiveComplete.archiveRequestId, List(ProgressEvent("Bag registered successfully")), Progress.Completed))
    .via(SnsPublishFlow(snsClient,progressSnsConfig, Some("registration_complete"))).map(_ => ())

  private def createStorageManifest(archiveComplete: ArchiveComplete)(
    implicit s3Client: AmazonS3): Either[ArchiveError[ArchiveComplete],
                                         (StorageManifest, ArchiveComplete)] =
    StorageManifestFactory
      .create(archiveComplete).map(manifest => (manifest, archiveComplete))

  private def updateStoredManifest(storageManifest: StorageManifest,
                                   requestContext: ArchiveComplete)(implicit ec: ExecutionContext) =
    dataStore.updateRecord(storageManifest.id.value)(
      ifNotExisting = (storageManifest, EmptyMetadata()))(
      ifExisting = (_, _) => (storageManifest, EmptyMetadata())
    ).map ( _ => (storageManifest, requestContext))
}
