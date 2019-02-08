package uk.ac.wellcome.platform.reindex.reindex_worker.services

import akka.Done
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.reindex.reindex_worker.models.{
  ReindexJobConfig,
  ReindexRequest
}
import uk.ac.wellcome.typesafe.Runnable

import scala.concurrent.{ExecutionContext, Future}

class ReindexWorkerService(
  recordReader: RecordReader,
  bulkSNSSender: BulkSNSSender,
  sqsStream: SQSStream[NotificationMessage],
  reindexJobConfigMap: Map[String, ReindexJobConfig]
)(implicit ec: ExecutionContext)
    extends Runnable {

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      reindexRequest: ReindexRequest <- Future.fromTry(
        fromJson[ReindexRequest](message.body))
      reindexJobConfig = reindexJobConfigMap.getOrElse(
        reindexRequest.jobConfigId,
        throw new RuntimeException(
          s"No such job config: ${reindexRequest.jobConfigId}")
      )
      recordsToSend: List[String] <- recordReader
        .findRecordsForReindexing(
          reindexParameters = reindexRequest.parameters,
          dynamoConfig = reindexJobConfig.dynamoConfig
        )
      _ <- bulkSNSSender.sendToSNS(
        messages = recordsToSend,
        snsConfig = reindexJobConfig.snsConfig
      )
    } yield ()

  def run(): Future[Done] =
    sqsStream.foreach(this.getClass.getSimpleName, processMessage)
}
