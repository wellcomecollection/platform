package uk.ac.wellcome.messaging.sns

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import com.google.inject.Inject
import grizzled.slf4j.Logging

import scala.concurrent.{blocking, ExecutionContext, Future}

case class PublishAttempt(id: Either[Throwable, String])

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

  private def toPublishRequest(message: String, subject: String) =
    new PublishRequest(snsConfig.topicArn, message, subject)
}
