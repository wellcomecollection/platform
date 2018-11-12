package uk.ac.wellcome.platform.reindex.reindex_worker.services

import akka.Done
import akka.actor.ActorSystem
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.reindex.reindex_worker.models.ReindexJob
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.storage.vhs.HybridRecord

import scala.concurrent.{ExecutionContext, Future}

class ReindexWorkerService(
  recordReader: RecordReader,
  hybridRecordSender: HybridRecordSender,
  sqsStream: SQSStream[NotificationMessage]
)(implicit val actorSystem: ActorSystem, ec: ExecutionContext) {

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      reindexJob: ReindexJob <- Future.fromTry(
        fromJson[ReindexJob](message.body))
      outdatedRecords: List[HybridRecord] <- recordReader
        .findRecordsForReindexing(reindexJob)
      _ <- hybridRecordSender.sendToSNS(records = outdatedRecords)
    } yield ()

  def run(): Future[Done] =
    sqsStream.foreach(this.getClass.getSimpleName, processMessage)
}
