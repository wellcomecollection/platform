package uk.ac.wellcome.messaging.sns

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import grizzled.slf4j.Logging
import io.circe.Encoder
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.{blocking, ExecutionContext, Future}

/** Writes messages to SNS.  This class can write to a different topic on
  * every request.
  *
  */
class SNSMessageWriter(snsClient: AmazonSNS)(implicit ec: ExecutionContext)
    extends Logging {
  def writeMessage(message: String,
                   subject: String,
                   snsConfig: SNSConfig): Future[PublishAttempt] =
    Future {
      blocking {
        debug(s"Publishing message $message to ${snsConfig.topicArn}")
        snsClient.publish(
          new PublishRequest(snsConfig.topicArn, message, subject))
      }
    }.map { publishResult =>
        debug(
          s"Published message $message to ${snsConfig.topicArn} (${publishResult.getMessageId})")
        PublishAttempt(Right(message))
      }
      .recover {
        case e: Throwable =>
          error("Failed to publish message", e)
          throw e
      }

  def writeMessage[T](message: T, subject: String, snsConfig: SNSConfig)(
    implicit encoder: Encoder[T]): Future[PublishAttempt] =
    writeMessage(
      message = toJson[T](message).get,
      subject = subject,
      snsConfig = snsConfig
    )
}
