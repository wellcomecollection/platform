package uk.ac.wellcome.platform.reindex.reindex_worker.services

import com.amazonaws.SdkClientException
import com.google.inject.Inject
import io.circe.Encoder
import uk.ac.wellcome.messaging.sns.{PublishAttempt, SNSWriter}
import uk.ac.wellcome.platform.reindex.reindex_worker.exceptions.ReindexerException

import scala.concurrent.{ExecutionContext, Future}

class BulkSNSWriter @Inject()(snsWriter: SNSWriter)(
  implicit ec: ExecutionContext) {
  def sendToSNS[T](records: List[T])(
    implicit encoder: Encoder[T]): Future[List[PublishAttempt]] = {
    Future.sequence {
      records
        .map { t: T =>
          snsWriter
            .writeMessage(t, subject = this.getClass.getSimpleName)
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
