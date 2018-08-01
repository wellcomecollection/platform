package uk.ac.wellcome.platform.reindex.creator.services

import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.models.reindexer.ReindexRequest
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

class NotificationSender @Inject()(snsWriter: SNSWriter)(
  implicit ec: ExecutionContext) {
  def sendNotifications(recordIds: List[String],
                        desiredVersion: Int): Future[List[Unit]] = {
    Future.sequence {
      recordIds.map {
        sendIndividualNotification(_, desiredVersion = desiredVersion)
      }
    }
  }

  private def sendIndividualNotification(recordId: String,
                                         desiredVersion: Int): Future[Unit] = {
    val request = ReindexRequest(
      id = recordId,
      desiredVersion = desiredVersion
    )

    for {
      _ <- snsWriter.writeMessage(
        message = toJson(request).get,
        subject = this.getClass.getSimpleName
      )
    } yield ()
  }
}
