package uk.ac.wellcome.platform.reindex.reindex_worker.services

import akka.Done
import akka.actor.ActorSystem
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.reindex.reindex_worker.models.{ReindexJob, ReindexParameters}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.{ExecutionContext, Future}

class ReindexWorkerService(
  recordReader: RecordReader,
  bulkSNSSender: BulkSNSSender,
  sqsStream: SQSStream[NotificationMessage],
  dynamoConfig: DynamoConfig,
  snsConfig: SNSConfig
)(implicit val actorSystem: ActorSystem, ec: ExecutionContext) {

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      reindexParameters: ReindexParameters <- Future.fromTry(
        fromJson[ReindexParameters](message.body))
      reindexJob = ReindexJob(
        parameters = reindexParameters,
        dynamoConfig = dynamoConfig,
        snsConfig = snsConfig
      )
      recordsToSend: List[String] <- recordReader
        .findRecordsForReindexing(reindexJob)
      _ <- bulkSNSSender.sendToSNS(messages = recordsToSend)
    } yield ()

  def run(): Future[Done] =
    sqsStream.foreach(this.getClass.getSimpleName, processMessage)
}
