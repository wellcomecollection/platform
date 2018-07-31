package uk.ac.wellcome.platform.reindex.creator.services

import com.google.inject.Inject
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.models.reindexer.ReindexRequest
import uk.ac.wellcome.platform.reindex.creator.models.ReindexJob
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.{ExecutionContext, Future}

class NotificationSender @Inject()(snsWriter: SNSWriter)(
  implicit ec: ExecutionContext) {
  def sendNotifications(recordIds: List[String],
                        reindexJob: ReindexJob): Future[List[Unit]] = {
    Future.sequence {
      recordIds.map { recordId: String =>
        sendIndividualNotification(
          recordId = recordId,
          dynamoConfig = reindexJob.dynamoConfig,
          desiredVersion = reindexJob.desiredVersion
        )
      }
    }
  }

  private def sendIndividualNotification(recordId: String,
                                         dynamoConfig: DynamoConfig,
                                         desiredVersion: Int): Future[Unit] = {
    val request = ReindexRequest(
      id = recordId,
      desiredVersion = desiredVersion,
      tableName = dynamoConfig.table
    )

    for {
      _ <- snsWriter.writeMessage(
        message = toJson(request).get,
        subject = this.getClass.getSimpleName
      )
    } yield ()
  }
}
