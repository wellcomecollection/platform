package uk.ac.wellcome.sns

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SNSConfig
import uk.ac.wellcome.test.utils.SNSLocal

class SNSWriterTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with SNSLocal
    with IntegrationPatience {

  val topicArn = createTopicAndReturnArn("test-topic-name")
  val snsConfig = SNSConfig(topicArn)

  it(
    "should send a message with subject to the SNS client and return a publish attempt with the id of the request") {
    val snsWriter = new SNSWriter(snsClient, snsConfig)
    val message = "someMessage"
    val subject = "subject"
    val futurePublishAttempt = snsWriter.writeMessage(message, Some(subject))

    whenReady(futurePublishAttempt) { publishAttempt =>
      val messages = listMessagesReceivedFromSNS()
      messages should have size (1)
      messages.head.message shouldBe "someMessage"
      messages.head.subject shouldBe "subject"
      publishAttempt.id should be(Right(messages.head.messageId))
    }
  }

  it(
    "should send a message with no subject to the SNS client with the default subject") {
    val snsWriter = new SNSWriter(snsClient, snsConfig)
    val message = "someMessage"

    val futurePublishAttempt = snsWriter.writeMessage(message, None)

    whenReady(futurePublishAttempt) { _ =>
      val messages = listMessagesReceivedFromSNS()
      messages should have size (1)
      messages.head.subject shouldBe "subject-not-specified"
    }
  }

  it("should return a failed future if it fails to publish the message") {
    val snsWriter =
      new SNSWriter(snsClient, SNSConfig("not a valid topic"))

    val futurePublishAttempt =
      snsWriter.writeMessage("someMessage", Some("subject"))

    whenReady(futurePublishAttempt.failed) { exception =>
      exception.getMessage should not be (empty)
    }
  }
}
