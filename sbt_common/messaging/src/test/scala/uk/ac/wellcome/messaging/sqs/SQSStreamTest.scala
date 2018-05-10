package uk.ac.wellcome.messaging.sqs

import java.util.concurrent.ConcurrentLinkedQueue

import org.mockito.Matchers.{any, anyDouble, endsWith, eq => equalTo}
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
import scala.concurrent.duration._

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
        val exampleObject1 = ExampleObject("Example value 1")
        sendMessage(queue, exampleObject1)

        val exampleObject2 = ExampleObject("Example value 2")
        sendMessage(queue, exampleObject2)

        val received = new ConcurrentLinkedQueue[ExampleObject]()

        messageStream.foreach(
          streamName = "test-stream",
          process = process(received))

        eventually {
          received should contain theSameElementsAs List(
            exampleObject1,
            exampleObject2)

          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)
        }
    }

  }

  it("increments *_ProcessMessage metric when successful") {
    withSQSStreamFixtures {
      case (messageStream, QueuePair(queue, _), metricsSender) =>
        val exampleObject = ExampleObject("An example value")
        sendMessage(queue, exampleObject)

        val received = new ConcurrentLinkedQueue[ExampleObject]()
        messageStream.foreach(
          streamName = "test-stream",
          process = process(received))

        eventually {
          verify(metricsSender, times(1)).timeAndCount(
            equalTo("test-stream_ProcessMessage"),
            any[() => Future[Unit]]())
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
            .incrementCount(endsWith("_MessageProcessingFailure"), anyDouble())
          received shouldBe empty

          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, size = 1)
        }
    }
  }

  it("continues reading if processing of some messages fails") {
    withSQSStreamFixtures {
      case (messageStream, QueuePair(queue, dlq), _) =>
        val exampleObject1 = ExampleObject("some value 1")
        val exampleObject2 = ExampleObject("some value 2")

        sqsClient.sendMessage(
          queue.url,
          "not valid json"
        )

        sendMessage(queue, exampleObject1)

        sqsClient.sendMessage(
          queue.url,
          "another not valid json"
        )

        sendMessage(queue, exampleObject2)

        val received = new ConcurrentLinkedQueue[ExampleObject]()
        messageStream.foreach(
          streamName = "test-stream",
          process = process(received))

        eventually {
          received should contain theSameElementsAs List(
            exampleObject1,
            exampleObject2)

          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, size = 2)
        }
    }
  }

  private def sendMessage(queue: Queue, obj: ExampleObject) =
    sqsClient.sendMessage(queue.url, toJson(obj).get)

  def withSQSStreamFixtures[R](
    testWith: TestWith[(SQSStream[ExampleObject],
                        QueuePair,
                        MetricsSender),
                       R]) = {

    withActorSystem { actorSystem =>
      withLocalSqsQueueAndDlq {
        case queuePair @ QueuePair(queue, _) =>
          withMockMetricSender { metricsSender =>
            val sqsConfig = SQSConfig(
              queueUrl = queue.url,
              waitTime = 1 millisecond,
              maxMessages = 1
            )

            val stream = new SQSStream[ExampleObject](
              actorSystem = actorSystem,
              sqsClient = asyncSqsClient,
              sqsConfig = sqsConfig,
              metricsSender = metricsSender)
            testWith((stream, queuePair, metricsSender))
          }
      }
    }
  }
}
