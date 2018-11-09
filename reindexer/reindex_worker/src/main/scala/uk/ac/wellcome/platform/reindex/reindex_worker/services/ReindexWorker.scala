package uk.ac.wellcome.platform.reindex.reindex_worker.services

import akka.actor.ActorSystem
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
  hybridRecordSender: HybridRecordSender,
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage],
  @Flag[String]("reindexer.tableMetadata") tableMetadata: String
)(implicit ec: ExecutionContext) {
  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      reindexJob: ReindexJob <- Future.fromTry(
        fromJson[ReindexJob](message.body))
      outdatedRecords: List[HybridRecord] <- recordReader
        .findRecordsForReindexing(reindexJob)
      _ <- hybridRecordSender.sendToSNS(records = outdatedRecords)
    } yield ()

  def stop() = system.terminate()
}
