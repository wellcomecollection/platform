package uk.ac.wellcome.sns

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SNSConfig
import uk.ac.wellcome.test.fixtures.SNS

class SNSWriterTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with SNS
    with IntegrationPatience {

//  val topicArn = createTopicAndReturnArn("test-topic-name")
//

  it(
    "should send a message with subject to the SNS client and return a publish attempt with the id of the request") {
    withLocalSnsTopic { arn =>
      val snsConfig = SNSConfig(arn)
      val snsWriter = new SNSWriter(snsClient, snsConfig)
      val message = "sns-writer-test-message"
      val subject = "sns-writer-test-subject"
      val futurePublishAttempt = snsWriter.writeMessage(
        message = message,
        subject = subject
      )

      whenReady(futurePublishAttempt) { publishAttempt =>
        val messages = listMessagesReceivedFromSNS(arn)
        messages should have size (1)
        messages.head.message shouldBe message
        messages.head.subject shouldBe subject
        publishAttempt.id should be(Right(messages.head.messageId))
      }
    }
  }

  it("should return a failed future if it fails to publish the message") {
    val snsWriter =
      new SNSWriter(snsClient, SNSConfig("not a valid topic"))

    val futurePublishAttempt =
      snsWriter.writeMessage(message = "someMessage", subject = "subject")

    whenReady(futurePublishAttempt.failed) { exception =>
      exception.getMessage should not be (empty)
    }
  }
}
