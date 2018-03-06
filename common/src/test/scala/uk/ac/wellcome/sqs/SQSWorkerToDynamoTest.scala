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
import org.scalatest.fixture.FunSpec
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{SQSConfig, SQSMessage}
import uk.ac.wellcome.utils.GlobalExecutionContext.context
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.fixtures._

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.duration._
import org.scalatest.Outcome

case class TestObject(foo: String)

class SQSWorkerToDynamoTest
    extends FunSpec
    with MockitoSugar
    with Matchers
    with Eventually
    with SqsFixtures {

  val mockPutMetricDataResult = mock[PutMetricDataResult]
  val mockCloudWatch = mock[AmazonCloudWatch]

  when(mockCloudWatch.putMetricData(any())).thenReturn(mockPutMetricDataResult)

  private val metricsSender: MetricsSender =
    new MetricsSender("namespace", mockCloudWatch, ActorSystem())

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
      println(s"$this.terminalFailureHook()")
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


  object TestWorkerFixtures extends TestWith[OneArgTest, Outcome] with AkkaFixtures {

    override def apply(test: OneArgTest) = {
      withActorSystem { actorSystem =>
        withLocalSqsQueue { queueUrl =>
          sqsClient.setQueueAttributes(queueUrl, Map("VisibilityTimeout" -> "0"))

          val worker: TestWorker = new TestWorker(queueUrl, actorSystem)
          val terminalWorker: TerminalFailingTestWorker = new TerminalFailingTestWorker(queueUrl, actorSystem)
          val conditionalCheckFailingWorker: ConditionalCheckFailingTestWorker = new ConditionalCheckFailingTestWorker(queueUrl, actorSystem)

          try {
            withFixture(test.toNoArgTest(FixtureParam(worker, terminalWorker, conditionalCheckFailingWorker, queueUrl)))
          } finally {
            worker.stop()
            terminalWorker.stop()
            conditionalCheckFailingWorker.stop()
          }

        }
      }
    }

  }

  case class FixtureParam(worker: TestWorker, terminalWorker: TerminalFailingTestWorker, conditionalCheckFailingWorker: ConditionalCheckFailingTestWorker, queueUrl: String)

  override def withFixture(test: OneArgTest) = TestWorkerFixtures(test)

  it("processes messages") { fixtures =>
    sqsClient.sendMessage(fixtures.queueUrl, testMessageJson)

    eventually {
      fixtures.worker.processCalled shouldBe true
      fixtures.worker.terminalFailure shouldBe false
    }
  }

  it("fails gracefully when receiving a ConditionalCheckFailedException") { fixtures =>
    sqsClient.sendMessage(fixtures.queueUrl, testMessageJson)

    eventually {
      fixtures.conditionalCheckFailingWorker.terminalFailure shouldBe false
    }
  }

  it("fails gracefully when a conversion fails") { fixtures =>
    val invalidBodyTestMessage = SQSMessage(
      subject = Some("subject"),
      messageType = "messageType",
      topic = "topic",
      body = "not valid json",
      timestamp = "timestamp"
    )

    val invalidBodyTestMessageJson = toJson(invalidBodyTestMessage).get

    sqsClient.sendMessage(fixtures.queueUrl, invalidBodyTestMessageJson)

    eventually {
      fixtures.worker.terminalFailure shouldBe false
    }
  }

  it(
    "fails terminally when receiving an exception other than ConditionalCheckFailedException") { fixtures =>
    sqsClient.sendMessage(fixtures.queueUrl, testMessageJson)

    eventually {
      fixtures.terminalWorker.terminalFailure shouldBe true
    }
  }

}
