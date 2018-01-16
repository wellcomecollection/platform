package uk.ac.wellcome.sqs

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import io.circe
import org.mockito.Matchers.{any, anyDouble, anyString, contains}
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{SQSConfig, SQSMessage}
import uk.ac.wellcome.test.utils.SQSLocal
import uk.ac.wellcome.utils.JsonUtil

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import io.circe
import io.circe.generic.extras.auto._
import io.circe.parser.decode
import uk.ac.wellcome.circe._
import uk.ac.wellcome.utils.GlobalExecutionContext.context

import scala.util.Try

case class TestObject(foo: String)



class SQSWorkerToDynamoTest
  extends FunSpec
    with SQSLocal
    with MockitoSugar
    with ScalaFutures
    with Matchers
    with Eventually {

  val queueUrl = createQueueAndReturnUrl("worker-test-queue")

  val mockPutMetricDataResult = mock[PutMetricDataResult]
  val mockCloudWatch = mock[AmazonCloudWatch]

  when(mockCloudWatch.putMetricData(any())).thenReturn(mockPutMetricDataResult)

  private val metricsSender: MetricsSender = new MetricsSender("namespace", mockCloudWatch)

  sqsClient.setQueueAttributes(queueUrl, Map("VisibilityTimeout" -> "0"))

  var processCalled: Boolean = false
  var terminalFailure: Boolean = false

  val testMessage = SQSMessage(
    subject = Some("subject"),
    messageType = "messageType",
    topic = "topic",
    body = """{ "foo": "bar"}""",
    timestamp = "timestamp"
  )

  val testMessageJson = JsonUtil.toJson(testMessage).get

  class TestWorker
    extends SQSWorkerToDynamo[TestObject](
      new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
      ActorSystem(),
      metricsSender) {
    override lazy val totalWait = 1.second

    override def conversion(s: String): Either[circe.Error, TestObject] =
      decode[TestObject](s)

    override def process(record: TestObject): Future[Unit] = Future {
      processCalled = true
    }

    override def terminalFailureHook(): Unit = {
      terminalFailure = true
    }
  }

  class ConditionalCheckFailingTestWorker
    extends SQSWorkerToDynamo[TestObject](
      new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
      ActorSystem(),
      metricsSender) {
    override lazy val totalWait = 1.second

    override def conversion(s: String): Either[circe.Error, TestObject] =
      decode[TestObject](s)

    override def process(record: TestObject): Future[Unit] = Future {
      throw new ConditionalCheckFailedException("Wrong!")
    }

    override def terminalFailureHook(): Unit = {
      terminalFailure = true
    }
  }

  class TerminalFailingTestWorker
    extends SQSWorkerToDynamo[TestObject](
      new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
      ActorSystem(),
      metricsSender) {
    override lazy val totalWait = 1.second

    override def conversion(s: String): Either[circe.Error, TestObject] =
      decode[TestObject](s)

    override def process(record: TestObject): Future[Unit] = Future {
      throw new RuntimeException("Wrong!")
    }

    override def terminalFailureHook(): Unit = {
      terminalFailure = true
    }
  }

  val worker = new TestWorker()
  val failingWorker = new ConditionalCheckFailingTestWorker()
  val terminalFailingWorker = new TerminalFailingTestWorker()

  it("processes messages") {
    processCalled = false

    worker.runSQSWorker()

    sqsClient.sendMessage(queueUrl, testMessageJson)

    eventually {
      processCalled shouldBe true
    }
  }

  it("fails terminally when receiving an exception other than ConditionalCheckFailedException") {
    terminalFailure = false

    terminalFailingWorker.runSQSWorker()

    sqsClient.sendMessage(queueUrl, testMessageJson)

    Thread.sleep(terminalFailingWorker.totalWait.toMillis + 2000)

    terminalFailure shouldBe true
  }


  it("fails gracefully when receiving a ConditionalCheckFailedException") {
    terminalFailure = false

    failingWorker.runSQSWorker()

    sqsClient.sendMessage(queueUrl, testMessageJson)

    Thread.sleep(failingWorker.totalWait.toMillis + 2000)

    terminalFailure shouldBe false
  }

  it("fails gracefully when a conversion fails") {
    terminalFailure = false

    worker.runSQSWorker()

    val invalidBodyTestMessage = SQSMessage(
      subject = Some("subject"),
      messageType = "messageType",
      topic = "topic",
      body = "not valid json",
      timestamp = "timestamp"
    )

    val invalidBodyTestMessageJson = JsonUtil.toJson(invalidBodyTestMessage).get

    sqsClient.sendMessage(queueUrl, invalidBodyTestMessageJson)

    Thread.sleep(worker.totalWait.toMillis + 2000)

    terminalFailure shouldBe false
  }
}