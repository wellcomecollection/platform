package uk.ac.wellcome.platform.reindex.reindex_worker.services

import akka.actor.{ActorSystem, Terminated}
import com.google.inject.Inject
import com.twitter.app.Flag
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.reindex.reindex_worker.models.ReindexJob
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, HybridRecord, VHSIndexEntry}

import scala.concurrent.{ExecutionContext, Future}

case class MiroMetadata(showInCatalogueAPI: Boolean)

class ReindexWorker @Inject()(
  recordReader: RecordReader,
  vhsIndexEntrySender: VHSIndexEntrySender,
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  @Flag[String]("reindexer.tableMetadata") tableMetadata: String
)(implicit ec: ExecutionContext) {
  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      reindexJob: ReindexJob <- Future.fromTry(
        fromJson[ReindexJob](message.body))
      recordsToSend <- findRecordsForReindexing(reindexJob)
      _ <- vhsIndexEntrySender.sendToSNS(records = recordsToSend)
    } yield ()

  def stop(): Future[Terminated] = system.terminate()

  private def findRecordsForReindexing(reindexJob: ReindexJob) =
    tableMetadata match {
      case "MiroMetadata" => recordReader.findRecordsForReindexing[MiroMetadata](reindexJob)
      case _ => recordReader.findRecordsForReindexing[EmptyMetadata](reindexJob)
    }
}
