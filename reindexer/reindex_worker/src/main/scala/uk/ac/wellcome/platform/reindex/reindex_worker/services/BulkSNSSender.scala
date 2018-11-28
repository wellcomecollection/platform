package uk.ac.wellcome.platform.reindex.reindex_worker.services

import uk.ac.wellcome.messaging.sns.{
  PublishAttempt,
  SNSConfig,
  SNSMessageWriter
}

import scala.concurrent.{ExecutionContext, Future}

class BulkSNSSender(snsMessageWriter: SNSMessageWriter)(
  implicit ec: ExecutionContext) {
  def sendToSNS(messages: List[String],
                snsConfig: SNSConfig): Future[List[PublishAttempt]] = {
    Future.sequence {
      messages
        .map { message: String =>
          snsMessageWriter
            .writeMessage(
              message = message,
              subject = this.getClass.getSimpleName,
              snsConfig = snsConfig
            )
        }
    }
  }
}
