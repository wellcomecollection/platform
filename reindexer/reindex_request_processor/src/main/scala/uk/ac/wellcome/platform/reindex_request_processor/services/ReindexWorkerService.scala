package uk.ac.wellcome.platform.reindex_request_processor.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.reindexer.{ReindexRequest, ReindexableRecord}
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, VersionedHybridStore}
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Try

class ReindexWorkerService @Inject()(
  versionedHybridStore: VersionedHybridStore[ReindexableRecord,
                                             EmptyMetadata,
                                             ObjectStore[ReindexableRecord]],
  sqsStream: SQSStream[NotificationMessage],
  system: ActorSystem) {

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      _ <- Future.fromTry(
        Try {
          val reindexRequest = fromJson[ReindexRequest](message.Message).get
          versionedHybridStore.updateRecord(reindexRequest.id)((
            ReindexableRecord(reindexRequest.id, reindexRequest.desiredVersion),
            EmptyMetadata()))(
            (existingRecord, existingMetadata) =>
              if (existingRecord.reindexVersion > reindexRequest.desiredVersion) {
                (existingRecord, existingMetadata)
              } else {
                (
                  ReindexableRecord(
                    reindexRequest.id,
                    reindexRequest.desiredVersion),
                  EmptyMetadata())
            }
          )
        }
      )
    } yield ()

  def stop() = system.terminate()
}
