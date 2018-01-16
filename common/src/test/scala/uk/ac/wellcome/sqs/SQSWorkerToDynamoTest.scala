package uk.ac.wellcome.sqs

import akka.actor.ActorSystem
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult
import io.circe
import org.mockito.Matchers.{any, anyDouble, anyString, contains}
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.Eventually
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

case class TestObject(foo: String)

class SQSWorkerToDynamoTest
  extends FunSpec
    with SQSLocal
    with MockitoSugar
    with Matchers
    with Eventually {


  val queueUrl = createQueueAndReturnUrl("worker-test-queue")

  val mockPutMetricDataResult = mock[PutMetricDataResult]
  val mockCloudWatch = mock[AmazonCloudWatch]

  when(mockCloudWatch.putMetricData(any())).thenReturn(mockPutMetricDataResult)

  private val metricsSender: MetricsSender = new MetricsSender("namespace", mockCloudWatch)

  sqsClient.setQueueAttributes(queueUrl, Map("VisibilityTimeout" -> "0"))

  class TestWorker
    extends SQSWorkerToDynamo[TestObject](
      new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
      ActorSystem(),
      metricsSender) {
    override lazy val totalWait = 1.second

    override def conversion(s: String): Either[circe.Error, TestObject] =
      decode[TestObject](s)

    override def process(record: TestObject): Future[Unit] =
      Future.successful(())

  }

  val worker = new TestWorker()

  it("processes messages") {
    worker.runSQSWorker()

    val testMessage = SQSMessage(
      subject = Some("subject"),
      messageType = "messageType",
      topic = "topic",
      body = """{ foo: "bar"}""",
      timestamp = "timestamp"
    )

    val testMessageJson = JsonUtil.toJson(testMessage).get

    sqsClient.sendMessage(queueUrl, testMessageJson)

    eventually {
      true shouldBe true
    }
  }

  ignore("fails gracefully when receiving a ConditionalCheckFailedException") {

  }

  ignore("fails gracefully when a conversion fails") {

  }
}