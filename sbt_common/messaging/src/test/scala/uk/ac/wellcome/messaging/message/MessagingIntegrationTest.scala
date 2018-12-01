package uk.ac.wellcome.messaging.message

import java.util.concurrent.ConcurrentLinkedDeque

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.test.fixtures.TestWith

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MessagingIntegrationTest
    extends FunSpec
    with Matchers
    with Messaging
    with Eventually
    with IntegrationPatience {

  def createMessage(size: Int) = ExampleObject("a" * size)

  val smallMessage: ExampleObject = createMessage(size = 100)
  val largeMessage: ExampleObject = createMessage(size = 300000)

  val subject = "message-integration-test-subject"

  it("sends and receives a message <256KB") {
    assertMessagesCanBeSentAndReceived(List(smallMessage))
  }

  it("sends and receives a message >256KB") {
    assertMessagesCanBeSentAndReceived(List(largeMessage))
  }

  it("sends and receives messages with a mixture of sizes") {
    val sizes = List(10, 50, 100, 280000, 20, 290000)
    assertMessagesCanBeSentAndReceived(
      sizes.map { createMessage }
    )
  }

  private def assertMessagesCanBeSentAndReceived(
    messages: List[ExampleObject]) =
    withLocalStackMessageWriterMessageStream {
      case (messageStream, messageWriter) =>
        val receivedMessages = new ConcurrentLinkedDeque[ExampleObject]()

        messages.map { msg =>
          messageWriter.write(message = msg, subject = subject)
        }

        messageStream.foreach(
          "integration-test-stream",
          obj => Future { receivedMessages.push(obj) })
        eventually {
          receivedMessages should contain theSameElementsAs messages
        }
    }

  private def withLocalStackMessageWriterMessageStream[R](
    testWith: TestWith[(MessageStream[ExampleObject],
                        MessageWriter[ExampleObject]),
                       R]): R = {
    withLocalStackMessageStreamFixtures[R] {
      case (queue, messageStream) =>
        withLocalS3Bucket { bucket =>
          withLocalStackSnsTopic { topic =>
            withLocalStackSubscription(queue, topic) { _ =>
              withExampleObjectMessageWriter(bucket, topic, localStackSnsClient) {
                messageWriter =>
                  testWith((messageStream, messageWriter))
              }
            }
          }
        }
    }
  }

  def withLocalStackMessageStreamFixtures[R](
    testWith: TestWith[(Queue, MessageStream[ExampleObject]), R]): R =
    withActorSystem { implicit actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withLocalStackSqsQueue { queue =>
          withMessageStream[ExampleObject, R](queue, metricsSender) {
            messageStream =>
              testWith((queue, messageStream))
          }
        }
      }
    }
}
