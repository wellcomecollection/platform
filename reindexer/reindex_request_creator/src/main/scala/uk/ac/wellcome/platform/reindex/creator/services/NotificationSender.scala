package uk.ac.wellcome.platform.reindex.creator.services

import com.google.inject.Inject
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.models.reindexer.ReindexRequest
import uk.ac.wellcome.platform.reindex.creator.models.ReindexJob
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.vhs.HybridRecord

import scala.concurrent.{ExecutionContext, Future}

class NotificationSender @Inject()(snsWriter: SNSWriter)(
  implicit ec: ExecutionContext) {
  def sendNotifications(records: List[HybridRecord],
                        reindexJob: ReindexJob): Future[List[Unit]] = {
    Future.sequence {
      records.map { record: HybridRecord =>
        sendIndividualNotification(
          recordId = record.id,
          dynamoConfig = reindexJob.dynamoConfig
        )
      }
    }
  }

  private def sendIndividualNotification(recordId: String,
                                         dynamoConfig: DynamoConfig): Future[Unit] = {
    val request = ReindexRequest(
      id = recordId,
      tableName = dynamoConfig.table
    )

    for {
      _ <- snsWriter.writeMessage(
        message = request,
        subject = this.getClass.getSimpleName
      )
    } yield ()
  }
}
