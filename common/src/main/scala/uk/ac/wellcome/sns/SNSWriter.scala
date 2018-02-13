package uk.ac.wellcome.sns

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.models.aws.SNSConfig
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.concurrent.{Future, blocking}

case class PublishAttempt(id: Either[Throwable, String])

class SNSWriter @Inject()(snsClient: AmazonSNS, snsConfig: SNSConfig)
    extends Logging {
  val defaultSubject = "subject-not-specified"

  def writeMessage(message: String, subject: String): Future[PublishAttempt] =
    Future {
      blocking {
        info(
          s"about to publish message $message on the SNS topic ${snsConfig.topicArn}")
        snsClient.publish(toPublishRequest(message = message, subject = subject))
      }
    }.map { publishResult =>
        info(s"Published message ${publishResult.getMessageId}")
        PublishAttempt(Right(publishResult.getMessageId))
      }
      .recover {
        case e: Throwable =>
          error("Failed to publish message", e)
          throw e
      }

  private def toPublishRequest(message: String, subject) =
    new PublishRequest(snsConfig.topicArn, message, subject)
}
