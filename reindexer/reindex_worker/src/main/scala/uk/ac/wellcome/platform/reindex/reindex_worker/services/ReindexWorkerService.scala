package uk.ac.wellcome.platform.reindex.reindex_worker.services

import akka.Done
import akka.actor.ActorSystem
import uk.ac.wellcome.WorkerService
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.reindex.reindex_worker.models.ReindexJob
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

class ReindexWorkerService(
  recordReader: RecordReader,
  bulkSNSSender: BulkSNSSender,
  sqsStream: SQSStream[NotificationMessage]
)(implicit val actorSystem: ActorSystem, ec: ExecutionContext)
    extends WorkerService {

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      reindexJob: ReindexJob <- Future.fromTry(
        fromJson[ReindexJob](message.body))
      recordsToSend: List[String] <- recordReader
        .findRecordsForReindexing(reindexJob)
      _ <- bulkSNSSender.sendToSNS(messages = recordsToSend)
    } yield ()

  def run(): Future[Done] =
    sqsStream.foreach(this.getClass.getSimpleName, processMessage)
}
