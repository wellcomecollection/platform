package uk.ac.wellcome.platform.archive.common.messaging

import java.util.concurrent.ConcurrentLinkedQueue

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.Flow
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sqs.SQSConfig
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SQS.{Queue, QueuePair}
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.test.fixtures.TestWith
import uk.ac.wellcome.test.utils.ExtendedPatience

import scala.concurrent.duration._

class MessageStreamTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with Messaging
    with MockitoSugar
    with ExtendedPatience {

  it("does not delete failing messages") {
    withMessageStreamFixtures[Unit] {
      case (messageStream, queuePair, actorSystem, metricsSender) =>
        implicit val adapter = Logging(actorSystem.eventStream, "customLogger")

        val received = new ConcurrentLinkedQueue[ExampleObject]()
        val _ = sendExampleObjects(queuePair.queue, 1)

        val exampleFlow = Flow[ExampleObject].map(o => {
          throw new RuntimeException("failed")

          received.add(o)

          ()
        })

        implicit val system = actorSystem
        implicit val materializer = ActorMaterializer()

        messageStream.run("example-stream", exampleFlow)

        eventually {
          received shouldBe empty

          assertQueuePairSizes(queuePair, 0, 1)
          //
          //          verify(metricsSender, never)
          //            .countSuccess(endsWith("_ProcessMessage"))
          //
          //          verify(metricsSender, atLeastOnce)
          //            .countFailure(endsWith("_ProcessMessage"))
        }
    }
  }

  it("continues to process messages after a workflow failure") {
    withMessageStreamFixtures[Unit] {
      case (messageStream, QueuePair(queue, dlq), actorSystem, metricsSender) =>
        implicit val adapter = Logging(actorSystem.eventStream, "customLogger")

        val numberOfMessages = 10

        val exampleObjects = sendExampleObjects(queue, numberOfMessages)
        val failObject = exampleObjects(2)

        val received = new ConcurrentLinkedQueue[ExampleObject]()

        val exampleFlow = Flow[ExampleObject].map(o => {
          if (o == failObject) throw new RuntimeException("failed")

          received.add(o)

          ()
        })

        val decider: Supervision.Decider = {
          case _ => Supervision.Resume
        }

        implicit val system = actorSystem
        implicit val materializer = ActorMaterializer(
          ActorMaterializerSettings(system).withSupervisionStrategy(decider)
        )

        messageStream.run("example-stream", exampleFlow)

        eventually {
          received should contain theSameElementsAs (exampleObjects.toSet - failObject)

          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, 1)

          //          verify(metricsSender, times(numberOfMessages - 1))
          //            .countSuccess(endsWith("_ProcessMessage"))
          //
          //          verify(metricsSender, atLeastOnce)
          //            .countFailure(endsWith("_ProcessMessage"))
        }
    }
  }

  it("reads messages off a queue, processes it and deletes them") {
    withMessageStreamFixtures[Unit] {
      case (messageStream, QueuePair(queue, dlq), actorSystem, metricsSender) =>
        implicit val adapter = Logging(actorSystem.eventStream, "customLogger")

        val numberOfMessages = 3

        val exampleObjects = sendExampleObjects(queue, numberOfMessages)

        val received = new ConcurrentLinkedQueue[ExampleObject]()

        val exampleFlow = Flow[ExampleObject].map(o => {
          received.add(o)

          ()
        })

        implicit val system = actorSystem
        implicit val materializer = ActorMaterializer()

        messageStream.run("example-stream", exampleFlow)

        eventually {
          received should contain theSameElementsAs exampleObjects

          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)

          //          verify(metricsSender, times(numberOfMessages))
          //            .countSuccess(endsWith("_ProcessMessage"))
        }
    }
  }

  def withMessageStreamFixtures[R](
    testWith: TestWith[(MessageStream[ExampleObject, Unit],
                        QueuePair,
                        ActorSystem,
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

            testWith((stream, queuePair, actorSystem, metricsSender))
          }
      }
    }
  }

  private def sendExampleObjects(queue: Queue, count: Int) = {
    (1 to count)
      .map(i => ExampleObject(s"Example value $i"))
      .map(o => {
        sqsClient.sendMessage(queue.url, toJson(o).get)
        o
      })
  }
}
