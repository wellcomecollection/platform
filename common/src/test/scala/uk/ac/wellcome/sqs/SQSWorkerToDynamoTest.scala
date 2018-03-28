package uk.ac.wellcome.sqs

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
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{SQSConfig, SQSMessage}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.fixtures._

import scala.collection.JavaConversions._
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

  class TestWorker(queueUrl: String, system: ActorSystem)
      extends SQSWorkerToDynamo[TestObject](
        new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
        system,
        metricsSender) {

    override lazy val poll = 100 millisecond

    override implicit val decoder = Decoder[TestObject]

    var processCalled: Boolean = false
    var terminalFailure: Boolean = false

    override def store(record: TestObject): Future[Unit] = Future {
      processCalled = true
    }

    override def terminalFailureHook(): Unit = {
      terminalFailure = true
    }
  }

  type TestWorkerFactory = (String, ActorSystem) => TestWorker

  def defaultTestWorkerFactory(queueUrl: String, system: ActorSystem) =
    new TestWorker(queueUrl, system)

  def conditionalCheckFailingTestWorkerFactory(queueUrl: String,
                                               system: ActorSystem) =
    new TestWorker(queueUrl: String, system) {
      override def store(record: TestObject): Future[Unit] = Future {
        throw new ConditionalCheckFailedException("Wrong!")
      }
    }

  def terminalTestWorkerFactory(queueUrl: String, system: ActorSystem) =
    new TestWorker(queueUrl: String, system) {
      override def store(record: TestObject): Future[Unit] = Future {
        throw new RuntimeException("Wrong!")
      }
    }

  def withTestWorker[R](testWorkFactory: TestWorkerFactory =
                          defaultTestWorkerFactory)(
    testWith: TestWith[(TestWorker, String), R]) = {
    withActorSystem { system =>
      withLocalSqsQueue { queueUrl =>
        val worker = testWorkFactory(queueUrl, system)

        try {
          testWith((worker, queueUrl))
        } finally {
          worker.stop()
        }

      }
    }
  }

  it("processes messages") {
    withTestWorker() {
      case (worker, queueUrl) =>
        sqsClient.sendMessage(queueUrl, testMessageJson)

        eventually {
          worker.processCalled shouldBe true
          worker.terminalFailure shouldBe false
        }
    }
  }

  it("fails gracefully when receiving a ConditionalCheckFailedException") {
    withTestWorker(conditionalCheckFailingTestWorkerFactory) {
      case (worker, queueUrl) =>
        sqsClient.sendMessage(queueUrl, testMessageJson)

        eventually {
          worker.terminalFailure shouldBe false
        }
    }
  }

  it("fails gracefully when a conversion fails") {
    withTestWorker(terminalTestWorkerFactory) {
      case (worker, queueUrl) =>
        val invalidBodyTestMessage = SQSMessage(
          subject = Some("subject"),
          messageType = "messageType",
          topic = "topic",
          body = "not valid json",
          timestamp = "timestamp"
        )

        val invalidBodyTestMessageJson = toJson(invalidBodyTestMessage).get

        sqsClient.sendMessage(queueUrl, invalidBodyTestMessageJson)

        eventually {
          worker.terminalFailure shouldBe false
        }
    }
  }

  it(
    "fails terminally when receiving an exception other than ConditionalCheckFailedException") {
    withTestWorker(terminalTestWorkerFactory) {
      case (worker, queueUrl) =>
        sqsClient.sendMessage(queueUrl, testMessageJson)

        eventually {
          worker.terminalFailure shouldBe true
        }
    }
  }

}
