package uk.ac.wellcome.messaging.sqs

import java.util.concurrent.ConcurrentLinkedQueue

import akka.stream.scaladsl.Flow
import org.mockito.Matchers.endsWith
import org.mockito.Mockito.{never, times, verify}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SQS.{Queue, QueuePair}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future

class SQSStreamTest
    extends FunSpec
    with Matchers
    with Messaging
    with Akka
    with ScalaFutures
    with ExtendedPatience
    with MetricsSenderFixture {

  def process(list: ConcurrentLinkedQueue[ExampleObject])(o: ExampleObject) = {
    list.add(o)
    Future.successful(())
  }

  it("reads messages off a queue, processes them and deletes them") {
    withSQSStreamFixtures {
      case (messageStream, QueuePair(queue, dlq), _) =>
        sendExampleObjects(queue = queue, count = 3)

        val received = new ConcurrentLinkedQueue[ExampleObject]()

        messageStream.foreach(
          streamName = "test-stream",
          process = process(received))

        eventually {
          received should contain theSameElementsAs createExampleObjects(
            count = 3)

          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)
        }
    }

  }

  it("increments *_ProcessMessage metric when successful") {
    withSQSStreamFixtures {
      case (messageStream, QueuePair(queue, _), metricsSender) =>
        sendExampleObjects(queue = queue)

        val received = new ConcurrentLinkedQueue[ExampleObject]()
        messageStream.foreach(
          streamName = "test-stream",
          process = process(received))

        eventually {
          verify(metricsSender, times(1))
            .incrementCount("test-stream_ProcessMessage_success")
        }
    }
  }

  it("fails gracefully when the object cannot be deserialised") {
    withSQSStreamFixtures {
      case (messageStream, QueuePair(queue, dlq), metricsSender) =>
        sqsClient.sendMessage(
          queue.url,
          "not valid json"
        )

        val received = new ConcurrentLinkedQueue[ExampleObject]()

        messageStream.foreach(
          streamName = "test-stream",
          process = process(received))

        eventually {
          verify(metricsSender, never())
            .incrementCount(endsWith("_failure"))
          received shouldBe empty

          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, size = 1)
        }
    }
  }

  it(
    "sends a failure metric if it doesn't fail gracefully when processing a message") {
    withSQSStreamFixtures {
      case (messageStream, QueuePair(queue, dlq), metricsSender) =>
        val exampleObject = ExampleObject("some value 1")

        sendMessage(queue, exampleObject)
        def processFailing(o: ExampleObject) = {
          Future.failed(new RuntimeException("BOOOOM!"))
        }

        messageStream.foreach(
          streamName = "test-stream",
          process = processFailing)

        eventually {
          verify(metricsSender, times(3))
            .incrementCount(metricName = "test-stream_ProcessMessage_failure")
          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, size = 1)
        }
    }
  }

  it("continues reading if processing of some messages fails") {
    withSQSStreamFixtures {
      case (messageStream, QueuePair(queue, dlq), _) =>
        sendExampleObjects(queue = queue, start = 1)
        sqsClient.sendMessage(queue.url, "not valid json")

        sendExampleObjects(queue = queue, start = 2)
        sqsClient.sendMessage(queue.url, "another not valid json")

        val received = new ConcurrentLinkedQueue[ExampleObject]()
        messageStream.foreach(
          streamName = "test-stream",
          process = process(received))

        eventually {
          received should contain theSameElementsAs createExampleObjects(
            count = 2)

          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, size = 2)
        }
    }
  }

  describe("runStream") {
    it("processes messages off a queue ") {
      withSQSStreamFixtures {
        case (messageStream, QueuePair(queue, dlq), metricsSender) =>
          sendExampleObjects(queue = queue, start = 1, count = 2)

          val received = new ConcurrentLinkedQueue[ExampleObject]()

          messageStream.runStream(
            "test-stream",
            source =>
              source.via(Flow.fromFunction {
                case (message, t) =>
                  received.add(t)
                  message
              }))

          eventually {
            received should contain theSameElementsAs createExampleObjects(
              count = 2)

            assertQueueEmpty(queue)
            assertQueueEmpty(dlq)

            verify(metricsSender, times(2))
              .incrementCount("test-stream_ProcessMessage_success")
          }
      }
    }

    it("does not delete failed messages and sends a failure metric") {
      withSQSStreamFixtures {
        case (messageStream, QueuePair(queue, dlq), metricsSender) =>
          sendExampleObjects(queue = queue)

          messageStream.runStream(
            "test-stream",
            source =>
              source.via(
                Flow.fromFunction(_ => throw new RuntimeException("BOOOM!"))))

          eventually {
            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, 1)

            verify(metricsSender, times(3))
              .incrementCount("test-stream_ProcessMessage_failure")
          }
      }
    }

    it("continues reading if processing of some messages fails") {
      withSQSStreamFixtures {
        case (messageStream, QueuePair(queue, dlq), _) =>
          sendExampleObjects(queue = queue, start = 1)
          sqsClient.sendMessage(queue.url, "not valid json")

          sendExampleObjects(queue = queue, start = 2)
          sqsClient.sendMessage(queue.url, "another not valid json")

          val received = new ConcurrentLinkedQueue[ExampleObject]()
          messageStream.runStream(
            "test-stream",
            source =>
              source.via(Flow.fromFunction {
                case (message, t) =>
                  received.add(t)
                  message
              }))

          eventually {
            received should contain theSameElementsAs createExampleObjects(
              count = 2)

            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, size = 2)
          }
      }
    }
  }

  private def createExampleObjects(start: Int = 1,
                                   count: Int): List[ExampleObject] =
    (start to (start + count - 1)).map { i =>
      ExampleObject(s"Example value $i")
    }.toList

  private def sendExampleObjects(queue: Queue,
                                 start: Int = 1,
                                 count: Int = 1) =
    createExampleObjects(start = start, count = count).map { exampleObject =>
      sqsClient.sendMessage(queue.url, toJson(exampleObject).get)
    }

  private def sendMessage(queue: Queue, obj: ExampleObject) =
    sqsClient.sendMessage(queue.url, toJson(obj).get)

  def withSQSStreamFixtures[R](
    testWith: TestWith[(SQSStream[ExampleObject], QueuePair, MetricsSender),
                       R]) = {

    withActorSystem { actorSystem =>
      withLocalSqsQueueAndDlq {
        case queuePair @ QueuePair(queue, _) =>
          withMockMetricSender { metricsSender =>
            withSQSStream[ExampleObject, R](actorSystem, queue, metricsSender) {
              stream =>
                testWith((stream, queuePair, metricsSender))
            }
          }
      }
    }
  }
}
