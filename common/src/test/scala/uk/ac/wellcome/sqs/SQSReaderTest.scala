package uk.ac.wellcome.sqs

import com.amazonaws.services.sqs.model.Message
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.aws.SQSConfig
import uk.ac.wellcome.test.utils.SQSLocal

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.duration._
import uk.ac.wellcome.utils.GlobalExecutionContext.context

class SQSReaderTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with SQSLocal {

  val queueUrl = createQueueAndReturnUrl("test_queue")
  // Setting 1 second timeout for tests, so that test don't have to wait too long to test message deletion
  sqsClient.setQueueAttributes(queueUrl, Map("VisibilityTimeout" -> "1"))

  it(
    "should get messages from the SQS queue, limited by the maximum number of messages and return them") {
    val sqsConfig =
      SQSConfig(queueUrl, waitTime = 20 seconds, maxMessages = 2)
    val messageStrings = List("someMessage1", "someMessage2", "someMessage3")
    messageStrings.foreach(sqsClient.sendMessage(queueUrl, _))
    val sqsReader =
      new SQSReader(sqsClient, sqsConfig)

    var receivedMessages: List[Message] = Nil

    val futureMessages = sqsReader.retrieveAndDeleteMessages(message => {
      receivedMessages = message :: receivedMessages
      Future.successful(())
    })

    whenReady(futureMessages) { _ =>
      eventually {
        receivedMessages should have size 2
        receivedMessages.foreach { message =>
          messageStrings should contain(message.getBody)
        }
        receivedMessages.head should not be equal(receivedMessages.tail.head)
      }
    }

    assertNumberOfMessagesAfterVisibilityTimeoutIs(1, sqsReader)
  }

  it("should return a failed future if reading from the SQS queue fails") {
    val sqsConfig =
      SQSConfig(
        "not a valid queue url",
        waitTime = 20 seconds,
        maxMessages = 1)
    val sqsReader = new SQSReader(sqsClient, sqsConfig)

    val futureMessages =
      sqsReader.retrieveAndDeleteMessages(_ => Future.successful(()))

    whenReady(futureMessages.failed) { exception =>
      exception.getMessage should not be (empty)
    }
  }

  it(
    "should return a failed future if processing one of the messages throws an exception - the failed message should not be deleted") {
    val sqsConfig =
      SQSConfig(queueUrl, waitTime = 20 seconds, maxMessages = 10)

    val failingMessage = "This message will fail"
    val messageStrings = List(
      "This is the first message",
      failingMessage,
      "This is the final message")
    messageStrings.foreach(sqsClient.sendMessage(queueUrl, _))
    val sqsReader =
      new SQSReader(sqsClient, sqsConfig)

    val futureMessages = sqsReader.retrieveAndDeleteMessages { message =>
      if (message.getBody == failingMessage)
        throw new RuntimeException(s"$failingMessage is not valid")
      else Future.successful(())
    }

    whenReady(futureMessages.failed) { exception =>
      exception shouldBe a[RuntimeException]
    }

    assertNumberOfMessagesAfterVisibilityTimeoutIs(1, sqsReader)
  }

  it(
    "should return a failed future if processing one of the messages returns a failed future - the failed message should not be deleted") {
    val sqsConfig =
      SQSConfig(queueUrl, waitTime = 20 seconds, maxMessages = 10)

    val failingMessage = "This message will fail"
    val messageStrings = List(
      "This is the first message",
      failingMessage,
      "This is the final message")
    messageStrings.foreach(sqsClient.sendMessage(queueUrl, _))
    val sqsReader =
      new SQSReader(sqsClient, sqsConfig)

    val futureMessages = sqsReader.retrieveAndDeleteMessages { message =>
      if (message.getBody == failingMessage)
        Future { throw new RuntimeException(s"$failingMessage is not valid") } else
        Future.successful(())
    }

    whenReady(futureMessages.failed) { exception =>
      exception shouldBe a[RuntimeException]
    }

    assertNumberOfMessagesAfterVisibilityTimeoutIs(1, sqsReader)
  }

  it(
    "should return a successful future but not delete the message if processing a message fails with GracefulFailureException") {
    val sqsConfig =
      SQSConfig(queueUrl, waitTime = 20 seconds, maxMessages = 10)

    val failingMessage = "This message will fail gracefully"
    val messageStrings = List(
      "This is the first message",
      failingMessage,
      "This is the final message")
    messageStrings.foreach(sqsClient.sendMessage(queueUrl, _))
    val sqsReader =
      new SQSReader(sqsClient, sqsConfig)

    val futureMessages = sqsReader.retrieveAndDeleteMessages { message =>
      if (message.getBody == failingMessage)
        Future {
          throw GracefulFailureException(
            new RuntimeException(s"$failingMessage is not valid"))
        } else
        Future.successful(())
    }

    whenReady(futureMessages) { _ =>
      // no need to assert anything. This is enough to assert that the future does not fail
    }

    assertNumberOfMessagesAfterVisibilityTimeoutIs(1, sqsReader)
  }

  private def assertNumberOfMessagesAfterVisibilityTimeoutIs(
    expectedNumberOfMessages: Int,
    sqsReader: SQSReader): Any = {
    // wait for the visibility period to expire
    Thread.sleep(1500)
    var receivedMessages: List[Message] = Nil
    val nextMessages = sqsReader.retrieveAndDeleteMessages(message => {
      receivedMessages = message :: receivedMessages
      Future.successful(())
    })
    whenReady(nextMessages) { _ =>
      receivedMessages should have size expectedNumberOfMessages
    }
  }
}
