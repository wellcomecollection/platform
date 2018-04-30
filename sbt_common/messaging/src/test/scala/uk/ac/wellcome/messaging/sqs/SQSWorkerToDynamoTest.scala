package uk.ac.wellcome.messaging.sqs

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import io.circe.Decoder
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import org.scalatest.Matchers
import org.scalatest.FunSpec
import uk.ac.wellcome.messaging.test.fixtures.SQS
import uk.ac.wellcome.messaging.test.fixtures.SQS.Queue
import uk.ac.wellcome.metrics.MetricsSender
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
    with SQS {

  val mockPutMetricDataResult = mock[PutMetricDataResult]
  val mockCloudWatch = mock[AmazonCloudWatch]

  when(mockCloudWatch.putMetricData(any())).thenReturn(mockPutMetricDataResult)

  private val metricsSender: MetricsSender =
    new MetricsSender(
      "namespace",
      100 milliseconds,
      mockCloudWatch,
      ActorSystem())

  val testMessage = TestSqsMessage()

  val testMessageJson = toJson(testMessage).get

  class TestWorker(queue: Queue, system: ActorSystem)
      extends SQSWorkerToDynamo[TestObject](
        new SQSReader(sqsClient, SQSConfig(queue.url, 1.second, 1)),
        system,
        metricsSender) {

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

  type TestWorkerFactory = (Queue, ActorSystem) => TestWorker

  def defaultTestWorkerFactory(queue: Queue, system: ActorSystem) =
    new TestWorker(queue, system)

  def conditionalCheckFailingTestWorkerFactory(queue: Queue,
                                               system: ActorSystem) =
    new TestWorker(queue, system) {
      override def store(record: TestObject): Future[Unit] = Future {
        throw new ConditionalCheckFailedException("Wrong!")
      }
    }

  def terminalTestWorkerFactory(queue: Queue, system: ActorSystem) =
    new TestWorker(queue, system) {
      override def store(record: TestObject): Future[Unit] = Future {
        throw new RuntimeException("Wrong!")
      }
    }

  def withTestWorker[R](testWorkFactory: TestWorkerFactory)(
    system: ActorSystem,
    queue: Queue)(testWith: TestWith[TestWorker, R]) = {
    val worker = testWorkFactory(queue, system)

    try {
      testWith(worker)
    } finally {
      worker.stop()
    }
  }

  def withFixtures[R](
    testWorkFactory: TestWorkerFactory = defaultTestWorkerFactory) =
    withActorSystem[R] and
      withLocalSqsQueue[R] and
      withTestWorker[R](testWorkFactory) _

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

  it("fails gracefully when receiving a ConditionalCheckFailedException") {
    withFixtures(conditionalCheckFailingTestWorkerFactory) {
      case (_, queue, worker) =>
        sqsClient.sendMessage(queue.url, testMessageJson)

        eventually {
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

  it(
    "fails terminally when receiving an exception other than ConditionalCheckFailedException") {
    withFixtures(terminalTestWorkerFactory) {
      case (_, queue, worker) =>
        sqsClient.sendMessage(queue.url, testMessageJson)

        eventually {
          worker.terminalFailure shouldBe true
        }
    }
  }

}
