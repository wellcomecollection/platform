package uk.ac.wellcome.messaging.sns

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import com.google.inject.Inject
import grizzled.slf4j.Logging
import io.circe.Encoder
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.{blocking, ExecutionContext, Future}

/** Writes messages to SNS.  This class can write to a different topic on
  * every request.
  *
  */
class SNSMessageWriter @Inject()(snsClient: AmazonSNS)(
  implicit ec: ExecutionContext)
    extends Logging {
  def writeMessage(message: String,
                   subject: String,
                   topicArn: String): Future[PublishAttempt] =
    Future {
      blocking {
        debug(s"Publishing message $message to $topicArn")
        snsClient.publish(new PublishRequest(topicArn, message, subject))
      }
    }.map { publishResult =>
        debug(
          s"Published message $message to $topicArn (${publishResult.getMessageId})")
        PublishAttempt(Right(message))
      }
      .recover {
        case e: Throwable =>
          error("Failed to publish message", e)
          throw e
      }

  def writeMessage[T](message: T, subject: String, topicArn: String)(
    implicit encoder: Encoder[T]): Future[PublishAttempt] =
    writeMessage(
      message = toJson[T](message).get,
      topicArn = topicArn,
      subject = subject
    )
}
