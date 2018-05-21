package uk.ac.wellcome.messaging.message

import java.util.concurrent.ConcurrentLinkedQueue

import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.Future

class MessagingIntegrationTest
    extends FunSpec
    with Matchers
    with Messaging
    with Eventually
    with ExtendedPatience {

  val message = ExampleObject("A message sent in the MessagingIntegrationTest")
  val subject = "message-integration-test-subject"

  def process(list: ConcurrentLinkedQueue[ExampleObject])(o: ExampleObject) = {
    list.add(o)
    Future.successful(())
  }

  it("sends and receives messages via writer/stream") {
    withLocalStackMessageStream {
      case (messageStream, messageWriter, queuePair) =>
        messageWriter.write(message = message, subject = subject)

        val received = new ConcurrentLinkedQueue[ExampleObject]()

        messageStream.foreach(
          streamName = "test-stream",
          process = process(received))

        eventually {
          received should contain theSameElementsAs List(message)

          assertQueueEmpty(queuePair.queue)
          assertQueueEmpty(queuePair.dlq)
        }
    }
  }

  it("sends and receives messages via writer/worker") {
    withLocalStackMessageWriter {
      case (worker, messageWriter) =>
        messageWriter.write(message = message, subject = subject)

        eventually {
          worker.calledWith shouldBe Some(message)
        }
    }
  }

  private def withLocalStackMessageStream[R](
    testWith: TestWith[
      (MessageStream[ExampleObject],
       MessageWriter[ExampleObject, S3TypeMessageSender[ExampleObject]],
       QueuePair),
      R]) = {

    withMessageStreamFixtures {
      case (bucket, messageStream, QueuePair(queue, dlq), _) =>
        withLocalStackSnsTopic { topic =>
          withLocalStackSubscription(queue, topic) { _ =>
            withMessageWriter(bucket, topic, localStackSnsClient) {
              messageWriter =>
                testWith((messageStream, messageWriter, QueuePair(queue, dlq)))
            }
          }
        }
    }
  }

  private def withLocalStackMessageWriter[R](
    testWith: TestWith[
      (ExampleMessageWorker,
       MessageWriter[ExampleObject, S3TypeMessageSender[ExampleObject]]),
      R]) = {
    withExampleObjectMessageWorkerFixtures {
      case (metricsSender, queue, bucket, worker) =>
        withLocalStackSnsTopic { topic =>
          withLocalStackSubscription(queue, topic) { _ =>
            withMessageWriter(bucket, topic, localStackSnsClient) {
              messageWriter =>
                testWith((worker, messageWriter))
            }
          }
        }
    }
  }
}
