package uk.ac.wellcome.messaging.sns

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import com.google.inject.Inject
import grizzled.slf4j.Logging
import io.circe.Encoder
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.{blocking, ExecutionContext, Future}

case class PublishAttempt(id: Either[Throwable, String])

/** Writes messages to SNS.  This class is configured with a single topic in
  * `snsConfig`, and writes to the same topic on every request.
  *
  */
class SNSWriter @Inject()(snsClient: AmazonSNS, snsConfig: SNSConfig)(
  implicit ec: ExecutionContext)
    extends Logging {

  def writeMessage(message: String, subject: String): Future[PublishAttempt] =
    Future {
      blocking {
        debug(s"Publishing message $message to ${snsConfig.topicArn}")
        snsClient.publish(
          toPublishRequest(message = message, subject = subject))
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

  def writeMessage[T](message: T, subject: String)(
    implicit encoder: Encoder[T]): Future[PublishAttempt] =
    writeMessage(
      message = toJson[T](message).get,
      subject = subject
    )

  private def toPublishRequest(message: String, subject: String) =
    new PublishRequest(snsConfig.topicArn, message, subject)
}
