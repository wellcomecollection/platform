package uk.ac.wellcome.messaging.sqs

import java.util.concurrent.ConcurrentLinkedDeque

import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import org.mockito.Mockito.{never, times, verify}
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.test.fixtures.{Messaging, SQS}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.test.fixtures.{Akka, TestWith}
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SQSToDynamoStreamTest
    extends FunSpec
    with SQS
    with Akka
    with MetricsSenderFixture
    with Messaging
    with ExtendedPatience {

  case class TestObject(foo: String)

  val testObject = TestObject(foo = "bar")

  def testMessageJson(obj: TestObject) =
    toJson(TestNotificationMessage(obj)).get

  private val streamName = "test-sqs-to-dynamo"
  it("processes messages") {
    withFixtures {
      case (mockMetricSender, QueuePair(queue, dlq), stream) =>
        sqsClient.sendMessage(queue.url, testMessageJson(testObject))
        val messages = new ConcurrentLinkedDeque[TestObject]()
        stream.foreach(streamName, obj => Future { messages.push(obj) })

        eventually {
          messages should contain only testObject
          verify(mockMetricSender, never())
            .incrementCount(s"${streamName}_MessageProcessingFailure", 1.0)
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)
        }
    }
  }

  it("fails gracefully when a conversion fails") {
    withFixtures {
      case (mockMetricSender, QueuePair(queue, dlq), stream) =>
        val invalidBodyTestMessage = NotificationMessage(
          Subject = "subject",
          MessageId = "message-id",
          TopicArn = "topic",
          Message = "not valid json"
        )

        val invalidBodyTestMessageJson = toJson(invalidBodyTestMessage).get

        sqsClient.sendMessage(queue.url, invalidBodyTestMessageJson)

        stream.foreach(streamName, _ => Future.successful(()))

        eventually {
          verify(mockMetricSender, never())
            .incrementCount(s"${streamName}_MessageProcessingFailure", 1.0)
          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, size = 1)
        }
    }
  }

  describe("with ConditionalCheckFailedException") {
    it("fails gracefully") {
      withFixtures {
        case (mockMetricSender, QueuePair(queue, dlq), stream) =>
          sqsClient.sendMessage(queue.url, testMessageJson(testObject))
          stream.foreach(
            streamName,
            _ => Future.failed(new ConditionalCheckFailedException("Wrong!")))

          eventually {
            verify(mockMetricSender, never())
              .incrementCount(s"${streamName}_MessageProcessingFailure", 1.0)
            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, size = 1)
          }
      }
    }
  }

  describe("without ConditionalCheckFailedException") {
    it("fails terminally") {
      withFixtures {
        case (mockMetricSender, QueuePair(queue, dlq), stream) =>
          sqsClient.sendMessage(queue.url, testMessageJson(testObject))

          stream.foreach(
            streamName,
            _ => Future.failed(new RuntimeException("Wrong!")))

          eventually {
            verify(mockMetricSender, times(3))
              .incrementCount(s"${streamName}_MessageProcessingFailure", 1.0)
            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, size = 1)
          }
      }
    }
  }

  def withFixtures[R](
    testWith: TestWith[(MetricsSender,
                        QueuePair,
                        SQSToDynamoStream[TestObject]),
                       R]): R =
    withActorSystem[R] { actorSystem =>
      withMockMetricSender { metrics =>
        withLocalSqsQueueAndDlq[R] { queuePair =>
          withSQSStream[NotificationMessage, R](
            actorSystem,
            queuePair.queue,
            metrics) { sqsStream =>
            val sqsToDynamoStream =
              new SQSToDynamoStream[TestObject](actorSystem, sqsStream)

            testWith((metrics, queuePair, sqsToDynamoStream))
          }
        }
      }
    }
}
