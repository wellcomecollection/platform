package uk.ac.wellcome.messaging.message

import java.util.concurrent.ConcurrentLinkedQueue

import akka.actor.ActorSystem
import org.mockito.Matchers.{any, anyDouble, endsWith, eq => equalTo}
import org.mockito.Mockito.{never, times, verify}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.storage.s3.{S3Config, S3ObjectLocation}
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.concurrent.duration._

class MessageStreamTest
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
    withMessageStreamFixtures {
      case (bucket, messageStream, QueuePair(queue, dlq), _) =>
        val exampleObject1 = ExampleObject("some value 1")
        sendMessage(bucket, queue, exampleObject1)
        val exampleObject2 = ExampleObject("some value 2")
        sendMessage(bucket, queue, exampleObject2)

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
    withMessageStreamFixtures {
      case (bucket, messageStream, QueuePair(queue, dlq), metricsSender) =>
        val exampleObject = ExampleObject("some value")

        sendMessage(bucket, queue, exampleObject)

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

  it("fails gracefully when NotificationMessage cannot be deserialised") {
    withMessageStreamFixtures {
      case (_, messageStream, QueuePair(queue, dlq), metricsSender) =>
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

  it("does not fail gracefully when the s3 object cannot be retrieved") {
    withMessageStreamFixtures {
      case (bucket, messageStream, QueuePair(queue, dlq), metricsSender) =>
        val streamName = "test-stream"

        val key = "key.json"

        // Do NOT put S3 object here

        val examplePointer =
          MessagePointer(S3ObjectLocation(bucket.name, key))
        val serialisedExamplePointer = toJson(examplePointer).get

        val exampleNotification = NotificationMessage(
          MessageId = "MessageId",
          TopicArn = "TopicArn",
          Subject = "Subject",
          Message = serialisedExamplePointer
        )

        val serialisedExampleNotification = toJson(exampleNotification).get

        sqsClient.sendMessage(
          queue.url,
          serialisedExampleNotification
        )

        val received = new ConcurrentLinkedQueue[ExampleObject]()

        messageStream.foreach(
          streamName = streamName,
          process = process(received))

        eventually {
          verify(metricsSender, times(3)).incrementCount(
            metricName = "test-stream_MessageProcessingFailure",
            count = 1.0)

          received shouldBe empty

          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, size = 1)
        }
    }
  }

  it("continues reading if processing of some messages fails ") {
    withMessageStreamFixtures {
      case (bucket, messageStream, QueuePair(queue, dlq), _) =>
        val exampleObject1 = ExampleObject("some value 1")
        val exampleObject2 = ExampleObject("some value 2")

        sqsClient.sendMessage(
          queue.url,
          "not valid json"
        )

        sendMessage(bucket, queue, exampleObject1)

        sqsClient.sendMessage(
          queue.url,
          "another not valid json"
        )

        sendMessage(bucket, queue, exampleObject2)

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

  def withMessageStreamFixtures[R](
    testWith: TestWith[(Bucket,
                        MessageStream[ExampleObject],
                        QueuePair,
                        MetricsSender),
                       R]) = {

      withActorSystem { actorSystem =>
          withLocalS3Bucket { bucket =>
            withLocalSqsQueueAndDlq {
              case queuePair@QueuePair(queue, _) =>
                withMockMetricSender { metricsSender =>
                  withMessageStream[ExampleObject,R](actorSystem, bucket, queue, metricsSender) { stream =>
                    testWith((bucket, stream, queuePair, metricsSender))
                  }

            }
          }
        }
      }
    }
}
