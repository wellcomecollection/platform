package uk.ac.wellcome.messaging.sqs

import akka.actor.ActorSystem
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import io.circe.Decoder
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import org.scalatest.Matchers
import org.scalatest.FunSpec
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.fixtures._

import scala.concurrent.Future
import scala.concurrent.duration._
import uk.ac.wellcome.test.utils.ExtendedPatience

case class TestObject(foo: String)

class SQSWorkerToDynamoTest
    extends FunSpec
    with MockitoSugar
    with Matchers
    with Eventually
    with ExtendedPatience
    with Akka
    with MetricsSenderFixture
    with SQS {

  val testMessage = TestSqsMessage()

  val testMessageJson = toJson(testMessage).get

  class TestWorker(metrics: MetricsSender, queue: Queue, system: ActorSystem)
      extends SQSWorkerToDynamo[TestObject](
        new SQSReader(sqsClient, SQSConfig(queue.url, 1.second, 1)),
        system,
        metrics) {

    override lazy val poll = 100 millisecond

    override implicit val decoder = Decoder[TestObject]

    var processCalled: Boolean = false
    var terminalFailure: Boolean = false

    override def store(record: TestObject): Future[Unit] = Future {
      processCalled = true
    }

    override def terminalFailureHook(exception: Throwable): Unit = {
      terminalFailure = true
    }
  }

  type TestWorkerFactory = (MetricsSender, Queue, ActorSystem) => TestWorker

  def defaultTestWorkerFactory(metrics: MetricsSender,
                               queue: Queue,
                               system: ActorSystem) =
    new TestWorker(metrics, queue, system)

  def conditionalCheckFailingTestWorkerFactory(metrics: MetricsSender,
                                               queue: Queue,
                                               system: ActorSystem) =
    new TestWorker(metrics, queue, system) {
      override def store(record: TestObject): Future[Unit] = Future {
        throw new ConditionalCheckFailedException("Wrong!")
      }
    }

  def terminalTestWorkerFactory(metrics: MetricsSender,
                                queue: Queue,
                                system: ActorSystem) =
    new TestWorker(metrics, queue, system) {
      override def store(record: TestObject): Future[Unit] = Future {
        throw new RuntimeException("Wrong!")
      }
    }

  def withTestWorker[R](testWorkFactory: TestWorkerFactory)(
    metrics: MetricsSender,
    queue: Queue,
    system: ActorSystem) =
    fixture[TestWorker, R](
      create = {
        testWorkFactory(metrics, queue, system)
      },
      destroy = { worker =>
        worker.stop()
      }
    )

  def withFixtures[R](
    testWorkFactory: TestWorkerFactory = defaultTestWorkerFactory
  )(testWith: TestWith[(ActorSystem, Queue, TestWorker), R]): R =
    withActorSystem[R] { actorSystem =>
      withMetricsSender(actorSystem) { metrics =>
        withLocalSqsQueue[R] { queue =>
          withTestWorker[R](testWorkFactory)(metrics, queue, actorSystem) {
            worker =>
              testWith((actorSystem, queue, worker))
          }
        }
      }
    }

  it("processes messages") {
    withFixtures() {
      case (_, queue, worker) =>
        sqsClient.sendMessage(queue.url, testMessageJson)

        eventually {
          worker.processCalled shouldBe true
          worker.terminalFailure shouldBe false
        }
    }
  }

  it("fails gracefully when a conversion fails") {
    withFixtures(terminalTestWorkerFactory) {
      case (_, queue, worker) =>
        val invalidBodyTestMessage = SQSMessage(
          subject = Some("subject"),
          messageType = "messageType",
          topic = "topic",
          body = "not valid json",
          timestamp = "timestamp"
        )

        val invalidBodyTestMessageJson = toJson(invalidBodyTestMessage).get

        sqsClient.sendMessage(queue.url, invalidBodyTestMessageJson)

        eventually {
          worker.terminalFailure shouldBe false
        }
    }
  }

  describe("with ConditionalCheckFailedException") {
    it("fails gracefully") {
      withFixtures(conditionalCheckFailingTestWorkerFactory) {
        case (_, queue, worker) =>
          sqsClient.sendMessage(queue.url, testMessageJson)

          eventually {
            worker.terminalFailure shouldBe false
          }
      }
    }
  }

  describe("without ConditionalCheckFailedException") {
    it("fails terminally") {
      withFixtures(terminalTestWorkerFactory) {
        case (_, queue, worker) =>
          sqsClient.sendMessage(queue.url, testMessageJson)

          eventually {
            worker.terminalFailure shouldBe true
          }
      }
    }
  }
}
