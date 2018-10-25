package uk.ac.wellcome.platform.reindex.reindex_worker.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.reindex.reindex_worker.models.ReindexJob
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.storage.vhs.HybridRecord

import scala.concurrent.{ExecutionContext, Future}

class ReindexWorker @Inject()(
  readerService: RecordReader,
  hybridRecordSender: HybridRecordSender,
  system: ActorSystem,
  sqsStream: SQSStream[NotificationMessage]
)(implicit ec: ExecutionContext) {
  sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      reindexJob: ReindexJob <- Future.fromTry(
        fromJson[ReindexJob](message.Message))
      outdatedRecords: List[HybridRecord] <- readerService
        .findRecordsForReindexing(reindexJob)
      _ <- hybridRecordSender.sendToSNS(records = outdatedRecords)
    } yield ()

  def stop() = system.terminate()
}
