package uk.ac.wellcome.platform.reindex.reindex_worker.services

import com.amazonaws.SdkClientException
import uk.ac.wellcome.messaging.sns.{PublishAttempt, SNSConfig, SNSMessageWriter}
import uk.ac.wellcome.platform.reindex.reindex_worker.exceptions.ReindexerException

import scala.concurrent.{ExecutionContext, Future}

class BulkSNSSender(snsMessageWriter: SNSMessageWriter)(implicit ec: ExecutionContext) {
  def sendToSNS(snsConfig: SNSConfig, messages: List[String]): Future[List[PublishAttempt]] = {
    Future.sequence {
      messages
        .map { message: String =>
          snsMessageWriter
            .writeMessage(
              message = message,
              subject = this.getClass.getSimpleName,
              topicArn = snsConfig.topicArn
            )
            .recover {

              // We've seen a lot of reindexer errors of the form:
              //
              //    Caused by: org.apache.http.conn.ConnectionPoolTimeoutException:
              //    Timeout waiting for connection from pool
              //
              // which logs a massive stack trace.  I'm trying to fix this by
              // tweaking the parallelism, but reducing it to a ReindexerException
              // should also reduce the amount of log spam.
              //
              case e: SdkClientException =>
                throw ReindexerException(e)
            }
        }
    }
  }
}
