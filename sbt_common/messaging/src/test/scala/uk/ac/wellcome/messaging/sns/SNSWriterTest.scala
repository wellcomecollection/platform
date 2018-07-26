package uk.ac.wellcome.messaging.sns

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.SNS
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic

class SNSWriterTest
    extends FunSpec
    with ScalaFutures
    with Matchers
    with SNS
    with IntegrationPatience {

  it("sends a message to SNS and returns a PublishAttempt") {
    withLocalSnsTopic { topic =>
      withSNSWriter(topic) { snsWriter =>
        val message = "sns-writer-test-message"
        val subject = "sns-writer-test-subject"
        val futurePublishAttempt = snsWriter.writeMessage(
          message = message,
          subject = subject
        )

        whenReady(futurePublishAttempt) { publishAttempt =>
          val messages = listMessagesReceivedFromSNS(topic)
          messages should have size 1
          messages.head.message shouldBe message
          messages.head.subject shouldBe subject
          publishAttempt.id should be(Right(messages.head.message))
        }
      }
    }
  }

  it("returns a failed Future if it fails to publish the message") {
    withSNSWriter(Topic("does-not-exist")) { snsWriter =>
      val futurePublishAttempt =
        snsWriter.writeMessage(message = "someMessage", subject = "subject")

      whenReady(futurePublishAttempt.failed) { exception =>
        exception.getMessage should not be empty
      }
    }
  }
}
