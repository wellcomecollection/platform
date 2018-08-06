package uk.ac.wellcome.platform.archiver

import java.util.concurrent.ConcurrentLinkedQueue

import akka.stream.scaladsl.Flow
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SQS.{Queue, QueuePair}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.platform.archiver.messaging.MessageStream
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.archiver.fixtures.AkkaS3

import scala.concurrent.duration._

class MessageStreamTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with Messaging
    with AkkaS3 {

  it("reads messages off a queue, processes them and deletes them") {
    withMessageStreamFixtures[Unit] {
      case (messageStream, QueuePair(queue, dlq), _) =>
        sendExampleObjects(queue = queue, start = 1, count = 3)

        val received = new ConcurrentLinkedQueue[ExampleObject]()

        val exampleFlow = Flow[ExampleObject].map(o => {
          received.add(o)

          ()
        })

        messageStream.run("example-stream", exampleFlow)

        eventually {
          received should contain theSameElementsAs createExampleObjects(
            start = 1,
            count = 3
          )

          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)
        }
    }
  }

  def withMessageStreamFixtures[R](
    testWith: TestWith[(MessageStream[ExampleObject, Unit],
                        QueuePair,
                        MetricsSender),
                       R]
  ) = {
    withActorSystem { actorSystem =>
      withLocalSqsQueueAndDlq {
        case queuePair @ QueuePair(queue, _) =>
          withMockMetricSender { metricsSender =>
            val sqsConfig = SQSConfig(
              queueUrl = queue.url,
              waitTime = 1 millisecond,
              maxMessages = 1
            )

            val stream = new MessageStream[ExampleObject, Unit](
              actorSystem = actorSystem,
              sqsClient = asyncSqsClient,
              sqsConfig = sqsConfig,
              metricsSender = metricsSender
            )

            testWith((stream, queuePair, metricsSender))
          }
      }
    }
  }

  private def createExampleObjects(
    start: Int,
    count: Int
  ): List[ExampleObject] =
    (start to (start + count - 1)).map { i =>
      ExampleObject(s"Example value $i")
    }.toList

  private def sendExampleObjects(queue: Queue, start: Int, count: Int) =
    createExampleObjects(start = start, count = count).map { exampleObject =>
      sqsClient.sendMessage(queue.url, toJson(exampleObject).get)
    }
}
