package uk.ac.wellcome.platform.reindex.creator.services

import com.google.inject.Inject
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.storage.vhs.HybridRecord

import scala.concurrent.{ExecutionContext, Future}

class NotificationSender @Inject()(snsWriter: SNSWriter)(
  implicit ec: ExecutionContext) {
  def sendNotifications(
    records: List[HybridRecord]): Future[List[PublishAttempt]] = {
    Future.sequence {
      records.map { record: HybridRecord =>
        snsWriter.writeMessage(
          message = record,
          subject = this.getClass.getSimpleName
        )
      }
    }
  }
}
