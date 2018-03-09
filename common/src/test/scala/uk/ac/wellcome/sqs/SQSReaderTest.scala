package uk.ac.wellcome.sqs

import com.amazonaws.services.sqs.model.Message
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.Matchers
import org.scalatest.FunSpec
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.aws.SQSConfig
import uk.ac.wellcome.test.fixtures._

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.duration._
import uk.ac.wellcome.utils.GlobalExecutionContext.context

class SQSReaderTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with Eventually
    with SqsFixtures {

  def withSqsReader[R](queueUrl: String, maxMessages: Int)(
    testWith: TestWith[SQSReader, R]) = {
    sqsClient.setQueueAttributes(queueUrl, Map("VisibilityTimeout" -> "1"))
    val sqsConfig = SQSConfig(queueUrl, waitTime = 20 seconds, maxMessages)
    val sqsReader = new SQSReader(sqsClient, sqsConfig)

    testWith(sqsReader)
  }

  def withInvalidSqsReader[R](testWith: TestWith[SQSReader, R]) = {
    val sqsConfig =
      SQSConfig("invalid-sqs-queue", waitTime = 20 seconds, maxMessages = 2)
    val sqsReader = new SQSReader(sqsClient, sqsConfig)

    testWith(sqsReader)
  }

  it(
    "should get messages from the SQS queue, limited by the maximum number of messages and return them") {
    withLocalSqsQueue { queueUrl =>
      withSqsReader(queueUrl, maxMessages = 2) { sqsReader =>
        val messageStrings =
          List("someMessage1", "someMessage2", "someMessage3")
        messageStrings.foreach(sqsClient.sendMessage(queueUrl, _))

        var receivedMessages: List[Message] = Nil

        val futureMessages = sqsReader.retrieveAndDeleteMessages(message => {
          synchronized {
            receivedMessages = message :: receivedMessages
          }
          Future.successful(())
        })

        whenReady(futureMessages) { _ =>
          eventually {
            receivedMessages should have size 2
            receivedMessages.foreach { message =>
              messageStrings should contain(message.getBody)
            }
          }

          whenReady(readMessagesAfterVisibilityTimeoutIs(sqsReader)) {
            _.size shouldBe 1
          }
        }
      }
    }
  }

  it("should return a failed future if reading from the SQS queue fails") {
    withInvalidSqsReader { sqsReader =>
      val futureMessages =
        sqsReader.retrieveAndDeleteMessages(_ => Future.successful(()))

      whenReady(futureMessages.failed) { exception =>
        exception.getMessage should not be (empty)
      }
    }
  }

  it(
    "should return a failed future if processing one of the messages throws an exception - the failed message should not be deleted") {
    withLocalSqsQueue { queueUrl =>
      withSqsReader(queueUrl, maxMessages = 10) { sqsReader =>
        val failingMessage = "This message will fail"
        val messageStrings = List(
          "This is the first message",
          failingMessage,
          "This is the final message")
        messageStrings.foreach(sqsClient.sendMessage(queueUrl, _))

        val futureMessages = sqsReader.retrieveAndDeleteMessages { message =>
          if (message.getBody == failingMessage)
            throw new RuntimeException(s"$failingMessage is not valid")
          else Future.successful(())
        }

        whenReady(futureMessages.failed) { exception =>
          exception shouldBe a[RuntimeException]
        }

        whenReady(readMessagesAfterVisibilityTimeoutIs(sqsReader)) {
          _.size shouldBe 1
        }
      }
    }
  }

  it(
    "should return a failed future if processing one of the messages returns a failed future - the failed message should not be deleted") {
    withLocalSqsQueue { queueUrl =>
      withSqsReader(queueUrl, maxMessages = 10) { sqsReader =>
        val failingMessage = "This message will fail"
        val messageStrings = List(
          "This is the first message",
          failingMessage,
          "This is the final message")
        messageStrings.foreach(sqsClient.sendMessage(queueUrl, _))

        val futureMessages = sqsReader.retrieveAndDeleteMessages { message =>
          if (message.getBody == failingMessage)
            Future {
              throw new RuntimeException(s"$failingMessage is not valid")
            } else
            Future.successful(())
        }

        whenReady(futureMessages.failed) { exception =>
          exception shouldBe a[RuntimeException]
        }

        whenReady(readMessagesAfterVisibilityTimeoutIs(sqsReader)) {
          _.size shouldBe 1
        }
      }
    }
  }

  it(
    "should return a successful future but not delete the message if processing a message fails with GracefulFailureException") {
    withLocalSqsQueue { queueUrl =>
      withSqsReader(queueUrl, maxMessages = 10) { sqsReader =>
        val failingMessage = "This message will fail gracefully"
        val messageStrings = List(
          "This is the first message",
          failingMessage,
          "This is the final message")
        messageStrings.foreach(sqsClient.sendMessage(queueUrl, _))

        val futureMessages = sqsReader.retrieveAndDeleteMessages { message =>
          if (message.getBody == failingMessage)
            Future {
              throw GracefulFailureException(
                new RuntimeException(s"$failingMessage is not valid"))
            } else Future.successful(())
        }

        whenReady(futureMessages) { _ =>
          // no need to assert anything. This is enough to assert that the future does not fail
        }

        whenReady(readMessagesAfterVisibilityTimeoutIs(sqsReader)) {
          _.size shouldBe 1
        }
      }
    }
  }

  private def readMessagesAfterVisibilityTimeoutIs(
    sqsReader: SQSReader): Future[List[Message]] = {
    // wait for the visibility period to expire
    Thread.sleep(1500)
    var receivedMessages: List[Message] = Nil
    val nextMessages = sqsReader.retrieveAndDeleteMessages(message => {
      synchronized {
        receivedMessages = message :: receivedMessages
      }
      Future.successful(())
    })

    for {
      _ <- nextMessages
    } yield receivedMessages
  }
}
