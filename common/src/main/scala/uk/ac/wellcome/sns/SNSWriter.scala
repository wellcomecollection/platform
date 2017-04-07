package uk.ac.wellcome.sns

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import com.google.inject.Inject
import com.twitter.inject.Logging
import uk.ac.wellcome.models.PublishAttempt
import uk.ac.wellcome.models.aws.SNSConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class SNSWriter @Inject()(snsClient: AmazonSNS, snsConfig: SNSConfig) extends Logging {
  val defaultSubject = "subject-not-specified"

  def writeMessage(message: String, subject: Option[String]): Future[PublishAttempt] = Future {
    info(s"about to publish message $message on the SNS topic ${snsConfig.topicArn}")
    snsClient.publish(new PublishRequest(snsConfig.topicArn, message, subject.getOrElse(defaultSubject)))}.map{publishResult =>
      PublishAttempt(publishResult.getMessageId)
    }
}
