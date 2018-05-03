package uk.ac.wellcome.messaging.sqs

import com.amazonaws.services.sqs.model.Message
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.Matchers
import org.scalatest.FunSpec
import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.duration._
import uk.ac.wellcome.utils.GlobalExecutionContext.context

class SQSReaderTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with Eventually
    with ExtendedPatience
    with SQS {

  def withSqsReader[R](maxMessages: Int)(queue: Queue)(
    testWith: TestWith[SQSReader, R]) = {
    val sqsConfig = SQSConfig(queue.url, waitTime = 20 seconds, maxMessages)
    val sqsReader = new SQSReader(sqsClient, sqsConfig)

    testWith(sqsReader)
  }

  def withInvalidSqsReader[R](testWith: TestWith[SQSReader, R]) = {
    val sqsConfig =
      SQSConfig("invalid-sqs-queue", waitTime = 20 seconds, maxMessages = 2)
    val sqsReader = new SQSReader(sqsClient, sqsConfig)

    testWith(sqsReader)
  }

  def withFixtures[R](maxMessages: Int)(testWith: TestWith[(Queue,SQSReader), R]): R  = withLocalSqsQueue[R] { q =>
    withSqsReader[R](maxMessages)(q) { reader =>
      testWith((q,reader))
    }
  }

  it("gets messages limited by the max messages") {
    withFixtures(maxMessages = 2) {
      case (queue, sqsReader) =>
        val messageStrings =
          List("someMessage1", "someMessage2", "someMessage3")
        messageStrings.foreach(sqsClient.sendMessage(queue.url, _))

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

  it("fails if reading from the SQS queue fails") {
    withInvalidSqsReader { sqsReader =>
      val futureMessages =
        sqsReader.retrieveAndDeleteMessages(_ => Future.successful(()))

      whenReady(futureMessages.failed) { exception =>
        exception.getMessage should not be (empty)
      }
    }
  }

  describe("the failed message is not deleted") {
    it("fails if processing one of the messages throws an exception") {
      withFixtures(maxMessages = 10) {
        case (queue, sqsReader) =>
          val failingMessage = "This message will fail"
          val messageStrings = List(
            "This is the first message",
            failingMessage,
            "This is the final message")
          messageStrings.foreach(sqsClient.sendMessage(queue.url, _))

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

    it("fails if processing one of the messages returns a failed future") {
      withFixtures(maxMessages = 10) {
        case (queue, sqsReader) =>
          val failingMessage = "This message will fail"
          val messageStrings = List(
            "This is the first message",
            failingMessage,
            "This is the final message")
          messageStrings.foreach(sqsClient.sendMessage(queue.url, _))

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

    it("succeeds if processing a message fails with GracefulFailureException") {
      withFixtures(maxMessages = 10) {
        case (queue, sqsReader) =>
          val failingMessage = "This message will fail gracefully"
          val messageStrings = List(
            "This is the first message",
            failingMessage,
            "This is the final message")
          messageStrings.foreach(sqsClient.sendMessage(queue.url, _))

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
