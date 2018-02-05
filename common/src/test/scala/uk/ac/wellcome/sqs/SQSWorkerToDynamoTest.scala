package uk.ac.wellcome.sqs

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import io.circe.Decoder
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{SQSConfig, SQSMessage}
import uk.ac.wellcome.test.utils.SQSLocal
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.duration._

case class TestObject(foo: String)

class SQSWorkerToDynamoTest
    extends FunSpec
    with SQSLocal
    with MockitoSugar
    with ScalaFutures
    with BeforeAndAfterAll
    with Matchers
    with Eventually {

  val mockPutMetricDataResult = mock[PutMetricDataResult]
  val mockCloudWatch = mock[AmazonCloudWatch]

  val actorSystem = ActorSystem()
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

  class TestWorker(queueUrl: String, system: ActorSystem)
      extends SQSWorkerToDynamo[TestObject](
        new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
        system,
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

  class ConditionalCheckFailingTestWorker(queueUrl: String,
                                          system: ActorSystem)
      extends TestWorker(queueUrl: String, system) {

    override def store(record: TestObject): Future[Unit] = Future {
      throw new ConditionalCheckFailedException("Wrong!")
    }
  }

  class TerminalFailingTestWorker(queueUrl: String, system: ActorSystem)
      extends TestWorker(queueUrl: String, system) {

    override def store(record: TestObject): Future[Unit] = Future {
      throw new RuntimeException("Wrong!")
    }
  }

  it("processes messages") {
    val queueUrl = newQueue("red")

    val worker = new TestWorker(queueUrl, actorSystem)

    worker.runSQSWorker()

    sqsClient.sendMessage(queueUrl, testMessageJson)

    eventually {
      worker.processCalled shouldBe true
      worker.terminalFailure shouldBe false
    }
  }

  it("fails gracefully when receiving a ConditionalCheckFailedException") {
    val queueUrl = newQueue("blue")

    val failingWorker =
      new ConditionalCheckFailingTestWorker(queueUrl, actorSystem)

    failingWorker.runSQSWorker()

    sqsClient.sendMessage(queueUrl, testMessageJson)

    Thread.sleep(failingWorker.totalWait.toMillis + 2000)

    failingWorker.terminalFailure shouldBe false
  }

  it("fails gracefully when a conversion fails") {
    val queueUrl = newQueue("green")

    val worker = new TestWorker(queueUrl, actorSystem)

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

    val terminalFailingWorker =
      new TerminalFailingTestWorker(queueUrl, actorSystem)

    terminalFailingWorker.runSQSWorker()

    sqsClient.sendMessage(queueUrl, testMessageJson)

    eventually {
      terminalFailingWorker.terminalFailure shouldBe true
    }
  }

  override def afterAll(): Unit = {
    super.afterAll()
    actorSystem.terminate()
  }
}
