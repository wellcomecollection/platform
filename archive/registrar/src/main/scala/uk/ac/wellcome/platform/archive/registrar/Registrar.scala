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
import uk.ac.wellcome.platform.archive.common.messaging.{MessageStream, NotificationParsingFlow, SnsPublishFlow}
import uk.ac.wellcome.platform.archive.common.models.{ArchiveComplete, NotificationMessage, RegistrationRequest}
import uk.ac.wellcome.platform.archive.common.modules.S3ClientConfig
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor
import uk.ac.wellcome.platform.archive.registrar.models._
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.s3.S3ClientFactory
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success}

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
                           executionContext: ExecutionContext
                         ) {
  def run() = {

    implicit val client = snsClient
    implicit val system = actorSystem

    implicit val adapter: LoggingAdapter =
      Logging(actorSystem.eventStream, "customLogger")

    val snsPublishFlow = SnsPublishFlow(snsClient, snsConfig)
    val notificationParsingFlow = NotificationParsingFlow[ArchiveComplete]()

    val flow = Flow[NotificationMessage]
      .via(notificationParsingFlow)
      .map(RegistrationRequest(_))
      .map(BagManifestFactory(s3Client)(_))
      .map {
        case (manifest, ctx) => updateStoredManifest(manifest)
      }
      .via(snsPublishFlow)
      .log("published notification")
    //      .filter {
    //        case (_, context) => context.callbackUrl.isDefined
    //      }

    messageStream.run("registrar", flow)
  }

  private def updateStoredManifest(manifest: BagManifest) = {
    dataStore.updateRecord(manifest.id.value)(
      ifNotExisting = (manifest, EmptyMetadata()))(
      ifExisting = (_, _) => (manifest, EmptyMetadata())
    )
  }
}
