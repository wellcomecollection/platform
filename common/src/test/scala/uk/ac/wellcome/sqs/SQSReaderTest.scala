package uk.ac.wellcome.sqs

import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.aws.SQSConfig
import uk.ac.wellcome.test.utils.SQSLocal

import scala.concurrent.duration._

class SQSReaderTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with SQSLocal {

  override def queueName: String = "id_minter_queue"

  it("should get messages from the SQS queue, limited by the maximum number of messages and return them") {
    val sqsConfig =
      SQSConfig("eu-west-1", queueUrl, waitTime = 20 seconds, maxMessages = 2)
    val messageStrings = List("someMessage1", "someMessage2", "someMessage3")
    messageStrings.foreach(sqsClient.sendMessage(queueUrl, _))
    val sqsReader =
      new SQSReader(sqsClient, sqsConfig)

    val futureMessages = sqsReader.retrieveAndDeleteMessages(identity)

    whenReady(futureMessages) { messages =>
      // SQS is not a FIFO queue and it only guarantees that a message is sent at least once,
      // not that it is received exactly once
      messages should have size 2
      messages.foreach { message =>
        messageStrings should contain(message.getBody)
      }
    }

    // Check that the previous 2 messages have been deleted
    assertNumberOfMessagesAfterVisibilityTimeoutIs(1, sqsReader)
  }

  it("should return a failed future if reading from the SQS queue fails") {
    val sqsConfig =
      SQSConfig("eu-west-1",
                "not a valid queue url",
                waitTime = 20 seconds,
                maxMessages = 1)
    val sqsReader = new SQSReader(sqsClient, sqsConfig)

    val futureMessages = sqsReader.retrieveAndDeleteMessages(identity)

    whenReady(futureMessages.failed) { exception =>
      exception.getMessage should not be (empty)
    }
  }

  it("should return a failed future if processing one of the messages fails - none of the messages should be deleted") {
    val sqsConfig =
      SQSConfig("eu-west-1", queueUrl, waitTime = 20 seconds, maxMessages = 10)

    val failingMessage = "failingMessage"
    val messageStrings = List("someMessage1", failingMessage, "someMessage3")
    messageStrings.foreach(sqsClient.sendMessage(queueUrl, _))
    val sqsReader =
      new SQSReader(sqsClient, sqsConfig)

    val futureMessages = sqsReader.retrieveAndDeleteMessages { message =>
      if (message.getBody == failingMessage)
        throw new RuntimeException(s"$failingMessage is not valid")
      else message
    }

    whenReady(futureMessages.failed) { exception =>
      exception shouldBe a[RuntimeException]
    }

    // Check that the queue still contains all 3 messages
    assertNumberOfMessagesAfterVisibilityTimeoutIs(3, sqsReader)
  }

  private def assertNumberOfMessagesAfterVisibilityTimeoutIs(
    expectedNumberOfMessages: Int,
    sqsReader: SQSReader): Any = {
    // this should be true after the visibility period expires
    //wait for the visibility period to expire
    Thread.sleep(1500)
    val nextMessages = sqsReader.retrieveAndDeleteMessages(identity)
    whenReady(nextMessages) { messages =>
      messages should have size expectedNumberOfMessages
    }
  }
}
