package uk.ac.wellcome.sqs

import com.twitter.inject.Logging
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.SQSMessage
import uk.ac.wellcome.models.Work
import uk.ac.wellcome.sns.{PublishAttempt, SNSWriter}
// import uk.ac.wellcome.sqs.SQSReaderGracefulException
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class SQSMessageReceiver(snsWriter: SNSWriter,
                         messageProcessor: (SQSMessage) => Try[Any],
                         metricsSender: MetricsSender)
    extends Logging {

  def receiveMessage(message: SQSMessage): Future[PublishAttempt] = {
    info(s"Starting to process message $message")
    metricsSender.timeAndCount(
      "ingest-time",
      () => {
        val processAttempt = messageProcessor(message)
        processAttempt match {
          case Success(s) =>
            publishMessage(s)
          case Failure(SQSReaderGracefulException(e)) =>
            info("Recoverable failure extracting workfrom record", e)
            Future.successful(PublishAttempt(Left(e)))
          case Failure(e) =>
            info("Unrecoverable failure extracting work from record", e)
            Future.failed(e)
        }
      }
    )
  }

  def publishMessage(message: Any): Future[PublishAttempt] =
    snsWriter.writeMessage(JsonUtil.toJson(message).get, Some("Foo"))
}
