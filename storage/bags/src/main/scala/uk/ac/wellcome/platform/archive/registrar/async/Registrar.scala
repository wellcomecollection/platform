package uk.ac.wellcome.platform.archive.registrar.async

import akka.Done
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.scaladsl.Flow
import akka.stream.{
  ActorAttributes,
  ActorMaterializer,
  ActorMaterializerSettings,
  Supervision
}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig}
import uk.ac.wellcome.platform.archive.common.flows.FoldEitherFlow
import uk.ac.wellcome.platform.archive.common.messaging.{
  MessageStream,
  NotificationParsingFlow
}
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError
import uk.ac.wellcome.platform.archive.common.models.ReplicationResult
import uk.ac.wellcome.platform.archive.registrar.async.factories.StorageManifestFactory
import uk.ac.wellcome.platform.archive.registrar.async.flows.{
  NotifyFailureFlow,
  UpdateStoredManifestFlow
}
import uk.ac.wellcome.platform.archive.registrar.async.models.BagManifestUpdate
import uk.ac.wellcome.platform.archive.registrar.common.models.StorageManifest
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.typesafe.Runnable

import scala.concurrent.{ExecutionContextExecutor, Future}

class Registrar(
  snsClient: AmazonSNS,
  progressSnsConfig: SNSConfig,
  s3Client: AmazonS3,
  messageStream: MessageStream[NotificationMessage, Unit],
  dataStore: VersionedHybridStore[StorageManifest,
                                  EmptyMetadata,
                                  ObjectStore[StorageManifest]]
)(implicit val actorSystem: ActorSystem)
    extends Logging
    with Runnable {
  def run(): Future[Done] = {
    implicit val snsclient = snsClient
    implicit val s3client = s3Client

    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")

    val decider: Supervision.Decider = { e =>
      error("Stream failure", e)
      Supervision.Resume
    }

    implicit val materializer: ActorMaterializer = ActorMaterializer(
      ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider)
    )
    implicit val executionContext: ExecutionContextExecutor =
      actorSystem.dispatcher

    val flow = Flow[NotificationMessage]
      .log("notification message")
      .via(NotificationParsingFlow[ReplicationResult])
      .map { result: ReplicationResult =>
        BagManifestUpdate(
          archiveRequestId = result.archiveRequestId,
          archiveBagLocation = result.srcBagLocation,
          accessBagLocation = result.dstBagLocation
        )
      }
      .map(createStorageManifest)
      .withAttributes(ActorAttributes.dispatcher(
        "akka.stream.materializer.blocking-io-dispatcher"))
      .log("created storage manifest")
      .via(
        FoldEitherFlow[
          ArchiveError[BagManifestUpdate],
          (StorageManifest, BagManifestUpdate),
          Unit](
          ifLeft = NotifyFailureFlow[BagManifestUpdate](
            "registrar_failure",
            progressSnsConfig)(_.archiveRequestId).map(_ => ()))(
          ifRight = UpdateStoredManifestFlow(dataStore, progressSnsConfig)))

    messageStream.run("registrar", flow)
  }

  private def createStorageManifest(bagManifestUpdate: BagManifestUpdate)(
    implicit s3Client: AmazonS3): Either[ArchiveError[BagManifestUpdate],
                                         (StorageManifest, BagManifestUpdate)] =
    StorageManifestFactory
      .create(bagManifestUpdate)
      .map(manifest => (manifest, bagManifestUpdate))

}
