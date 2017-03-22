package uk.ac.wellcome.models.aws

import uk.ac.wellcome.models.{
  PublishableMessage,
  PublishAttempt
}
import scala.util.Try

import javax.inject.Inject
import com.amazonaws.services.sns.model.{PublishRequest, PublishResult}
import com.amazonaws.services.sns.AmazonSNS
import com.twitter.inject.TwitterModule

case class SNSMessage(
  subject: Option[String] = None,
  body: String,
  topic: String,
  snsClient: AmazonSNS
) extends PublishableMessage {

  val defaultSubject = "subject-not-specified"

  def publish() =
    Try {
      snsClient.publish(
        new PublishRequest(topic, body, subject.getOrElse(defaultSubject))
      )
    }.map(r => PublishAttempt(r.getMessageId()))

}
