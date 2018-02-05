package uk.ac.wellcome.sqs

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.amazonaws.services.sqs.model.PurgeQueueRequest
import io.circe
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{SQSConfig, SQSMessage}
import uk.ac.wellcome.test.utils.SQSLocal
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import io.circe.Decoder
import uk.ac.wellcome.utils.GlobalExecutionContext.context

case class TestObject(foo: String)

class SQSWorkerToDynamoTest
    extends FunSpec
    with SQSLocal
    with MockitoSugar
    with ScalaFutures
    with BeforeAndAfterEach
    with Matchers
    with Eventually {

  val mockPutMetricDataResult = mock[PutMetricDataResult]
  val mockCloudWatch = mock[AmazonCloudWatch]

  when(mockCloudWatch.putMetricData(any())).thenReturn(mockPutMetricDataResult)

  private val metricsSender: MetricsSender =
    new MetricsSender("namespace", mockCloudWatch, ActorSystem())

  val testMessage = SQSMessage(
    subject = Some("subject"),
    messageType = "messageType",
    topic = "topic",
    body = """{ "foo": "bar"}""",
    timestamp = "timestamp"
  )

  val testMessageJson = toJson(testMessage).get

  def newQueue(name: String) = {
    val queueUrl = createQueueAndReturnUrl(name)
    sqsClient.setQueueAttributes(queueUrl, Map("VisibilityTimeout" -> "0"))

    queueUrl
  }

  class TestWorker(queueUrl: String)
      extends SQSWorkerToDynamo[TestObject](
        new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
        ActorSystem(),
        metricsSender) {
    override lazy val totalWait = 1.second

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

  class ConditionalCheckFailingTestWorker(queueUrl: String)
      extends TestWorker(queueUrl: String) {

    override def store(record: TestObject): Future[Unit] = Future {
      throw new ConditionalCheckFailedException("Wrong!")
    }
  }

  class TerminalFailingTestWorker(queueUrl: String)
      extends TestWorker(queueUrl: String) {

    override def store(record: TestObject): Future[Unit] = Future {
      throw new RuntimeException("Wrong!")
    }
  }

  it("processes messages") {
    val queueUrl = newQueue("red")

    val worker = new TestWorker(queueUrl)

    worker.runSQSWorker()

    sqsClient.sendMessage(queueUrl, testMessageJson)

    eventually {
      worker.processCalled shouldBe true
      worker.terminalFailure shouldBe false
    }
  }

  it("fails gracefully when receiving a ConditionalCheckFailedException") {
    val queueUrl = newQueue("blue")

    val failingWorker = new ConditionalCheckFailingTestWorker(queueUrl)

    failingWorker.runSQSWorker()

    sqsClient.sendMessage(queueUrl, testMessageJson)

    Thread.sleep(failingWorker.totalWait.toMillis + 2000)

    failingWorker.terminalFailure shouldBe false
  }

  it("fails gracefully when a conversion fails") {
    val queueUrl = newQueue("green")

    val worker = new TestWorker(queueUrl)

    worker.runSQSWorker()

    val invalidBodyTestMessage = SQSMessage(
      subject = Some("subject"),
      messageType = "messageType",
      topic = "topic",
      body = "not valid json",
      timestamp = "timestamp"
    )

    val invalidBodyTestMessageJson = toJson(invalidBodyTestMessage).get

    sqsClient.sendMessage(queueUrl, invalidBodyTestMessageJson)

    Thread.sleep(worker.totalWait.toMillis + 2000)

    worker.terminalFailure shouldBe false
  }

  it(
    "fails terminally when receiving an exception other than ConditionalCheckFailedException") {
    val queueUrl = newQueue("purple")

    val terminalFailingWorker = new TerminalFailingTestWorker(queueUrl)

    terminalFailingWorker.runSQSWorker()

    sqsClient.sendMessage(queueUrl, testMessageJson)

    eventually {
      terminalFailingWorker.terminalFailure shouldBe true
    }
  }
}
