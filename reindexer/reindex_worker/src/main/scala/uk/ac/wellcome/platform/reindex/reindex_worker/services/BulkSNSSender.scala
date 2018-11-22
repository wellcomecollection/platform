package uk.ac.wellcome.platform.reindex.reindex_worker.services

import uk.ac.wellcome.messaging.sns.{PublishAttempt, SNSWriter}

import scala.concurrent.{ExecutionContext, Future}

class BulkSNSSender(snsWriter: SNSWriter)(implicit ec: ExecutionContext) {
  def sendToSNS(messages: List[String]): Future[List[PublishAttempt]] = {
    Future.sequence {
      messages
        .map { message: String =>
          snsWriter
            .writeMessage(
              message = message,
              subject = this.getClass.getSimpleName
            )
        }
    }
  }
}
