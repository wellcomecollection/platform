package uk.ac.wellcome.messaging.message

import java.util.concurrent.ConcurrentLinkedDeque

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures._
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class MessagingIntegrationTest
    extends FunSpec
    with Matchers
    with Messaging
    with Eventually
    with IntegrationPatience {

  val message = ExampleObject("A message sent in the MessagingIntegrationTest")
  val subject = "message-integration-test-subject"

  it("sends and receives messages") {
    withLocalStackMessageWriterMessageStream {
      case (messageStream, messageWriter) =>
        val messages = new ConcurrentLinkedDeque[ExampleObject]()
        messageWriter.write(message = message, subject = subject)
        messageStream.foreach(
          "integration-test-stream",
          obj => Future { messages.push(obj) })
        eventually {
          messages should contain only message
        }
    }
  }

  private def withLocalStackMessageWriterMessageStream[R](
    testWith: TestWith[(MessageStream[ExampleObject],
                        MessageWriter[ExampleObject]),
                       R]) = {
    withLocalStackMessageStreamFixtures[R] {
      case (queue, bucket, messageStream) =>
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

  def withLocalStackMessageStreamFixtures[R](
    testWith: TestWith[(Queue, Bucket, MessageStream[ExampleObject]), R]) = {
    withMessagingActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withLocalS3Bucket { bucket =>
          withLocalStackSqsQueue { queue =>
            withMessageStream[ExampleObject, R](
              actorSystem,
              bucket,
              queue,
              metricsSender) { messageStream =>
              testWith((queue, bucket, messageStream))
            }
          }

        }

      }
    }
  }

}
