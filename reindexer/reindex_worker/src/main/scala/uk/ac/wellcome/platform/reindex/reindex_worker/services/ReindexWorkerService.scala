package uk.ac.wellcome.platform.reindex.reindex_worker.services

import akka.Done
import uk.ac.wellcome.WorkerService
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.reindex.reindex_worker.models.{
  ReindexJob,
  ReindexRequest
}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.{ExecutionContext, Future}

class ReindexWorkerService(
  recordReader: RecordReader,
  bulkSNSSender: BulkSNSSender,
  sqsStream: SQSStream[NotificationMessage],
  dynamoConfig: DynamoConfig,
  snsConfig: SNSConfig
)(implicit ec: ExecutionContext) extends WorkerService {

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      reindexRequest: ReindexRequest <- Future.fromTry(
        fromJson[ReindexRequest](message.body))
      reindexJob = ReindexJob(
        parameters = reindexRequest.parameters,
        dynamoConfig = dynamoConfig,
        snsConfig = snsConfig
      )
      recordsToSend: List[String] <- recordReader
        .findRecordsForReindexing(
          reindexParameters = reindexJob.parameters,
          dynamoConfig = reindexJob.dynamoConfig
        )
      _ <- bulkSNSSender.sendToSNS(
        messages = recordsToSend,
        snsConfig = reindexJob.snsConfig
      )
    } yield ()

  def run(): Future[Done] =
    sqsStream.foreach(this.getClass.getSimpleName, processMessage)
}
