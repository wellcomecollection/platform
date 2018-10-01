package uk.ac.wellcome.platform.archive.call_backerei

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
import uk.ac.wellcome.platform.archive.common.progress.flows.CallbackFlow
import uk.ac.wellcome.platform.archive.common.progress.monitor.ProgressMonitor
import uk.ac.wellcome.platform.archive.call_backerei.flows.SnsPublishFlow
import uk.ac.wellcome.platform.archive.call_backerei.models._
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.s3.S3ClientFactory
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

class CallBäckerei @Inject()(
  snsClient: AmazonSNSAsync,
  snsConfig: SNSConfig,
  messageStream: MessageStream[NotificationMessage, Object],
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

    val workFlow = Flow[NotificationMessage]
      .map(parseNotification)
      .via(CallbackFlow())
      .log("executed callback")

    messageStream.run("callBäckerei", workFlow)
  }

  private def parseNotification(message: NotificationMessage) = {
    fromJson[Progress](message.Message) match {
      case Success(progress: Progress) => progress
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
