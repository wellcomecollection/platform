package uk.ac.wellcome.platform.idminter.modules

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.PublishRequest
import uk.ac.wellcome.models.PublishAttempt
import uk.ac.wellcome.models.aws.SNSConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class SNSWriter(snsClient: AmazonSNS, snsConfig: SNSConfig) {
  val defaultSubject = "subject-not-specified"
  def writeMessage(message: String, subject: Option[String]): Future[PublishAttempt] = Future {
    snsClient.publish(new PublishRequest(snsConfig.topicArn, message, subject.getOrElse(defaultSubject)))}.map{publishResult =>
      PublishAttempt(publishResult.getMessageId)
    }
}
