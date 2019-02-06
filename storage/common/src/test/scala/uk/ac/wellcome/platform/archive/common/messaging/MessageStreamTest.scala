package uk.ac.wellcome.platform.archive.common.messaging

import java.util.concurrent.ConcurrentLinkedQueue

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.Flow
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.Messaging
import uk.ac.wellcome.messaging.fixtures.SQS.{Queue, QueuePair}
import uk.ac.wellcome.platform.archive.common.fixtures.ArchiveMessaging
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.test.fixtures.Akka

class MessageStreamTest
    extends FunSpec
    with Matchers
    with Akka
    with ScalaFutures
    with ArchiveMessaging
    with Messaging
    with MockitoSugar
    with IntegrationPatience {

  it("does not delete failing messages") {
    withMessageStreamFixtures[Unit] {
      case (messageStream, queuePair, actorSystem) =>
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
        }
    }
  }

  it("continues to process messages after a workflow failure") {
    withMessageStreamFixtures[Unit] {
      case (messageStream, QueuePair(queue, dlq), actorSystem) =>
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
        }
    }
  }

  it("reads messages off a queue, processes it and deletes them") {
    withMessageStreamFixtures[Unit] {
      case (messageStream, QueuePair(queue, dlq), actorSystem) =>
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
        }
    }
  }

  def withMessageStreamFixtures[R](
    testWith: TestWith[(MessageStream[ExampleObject, Unit],
                        QueuePair,
                        ActorSystem),
                       R]
  ): R =
    withActorSystem { implicit actorSystem =>
      withLocalSqsQueueAndDlq {
        case queuePair @ QueuePair(queue, _) =>
          withArchiveMessageStream[ExampleObject, Unit, R](queue) { stream =>
            testWith((stream, queuePair, actorSystem))
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
