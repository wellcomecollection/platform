package uk.ac.wellcome.models

import scala.util.Try

import javax.inject.Inject
import com.amazonaws.services.sns.model.{PublishRequest, PublishResult}
import com.amazonaws.services.sns.AmazonSNS
import com.twitter.inject.TwitterModule

case class PublishAttempt(id: String)

trait PublishableMessage {
  val subject: String
  val message: String
  val topic: String

  def publish(): Try[PublishAttempt]
}

case class SNSMessage(
  subject: String,
  message: String,
  topic: String,
  snsClient: AmazonSNS
) extends PublishableMessage {

  def publish() =
    Try {
      snsClient.publish(
        new PublishRequest(topic, message, subject)
      )
    }.map(r => PublishAttempt(r.getMessageId()))

}
