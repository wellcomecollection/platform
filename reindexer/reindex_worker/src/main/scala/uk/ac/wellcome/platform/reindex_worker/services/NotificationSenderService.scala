package uk.ac.wellcome.platform.reindex_worker.services

import com.google.inject.Inject
import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.platform.reindex_worker.models.ReindexRequest
import uk.ac.wellcome.utils.JsonUtil.toJson

import scala.concurrent.Future

class NotificationSenderService @Inject()(snsWriter: SNSWriter) {
  def sendNotifications(recordIds: List[String], desiredVersion: Int): Future[List[Unit]] = {
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
