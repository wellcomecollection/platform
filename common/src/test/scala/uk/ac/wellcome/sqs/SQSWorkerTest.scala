package uk.ac.wellcome.sqs

import akka.actor.ActorSystem
import org.mockito.Matchers.{any, anyDouble, anyString, contains}
import org.mockito.Mockito
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.FunSpec
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{SQSConfig, SQSMessage}
import uk.ac.wellcome.test.utils.SQSLocal
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.JavaConversions._

import uk.ac.wellcome.utils.GlobalExecutionContext.context

class SQSWorkerTest
    extends FunSpec
    with SQSLocal
    with MockitoSugar
    with Eventually {

  val queueUrl = createQueueAndReturnUrl("worker-test-queue")

  private val metricsSender: MetricsSender = mock[MetricsSender]

  when(
    metricsSender.timeAndCount[Unit](
      anyString(),
      any[() => Future[Unit]].apply
    )
  ).thenReturn(
    Future.successful()
  )

  sqsClient.setQueueAttributes(queueUrl, Map("VisibilityTimeout" -> "0"))

  class TestWorker
      extends SQSWorker(
        new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1)),
        ActorSystem(),
        metricsSender) {
    override lazy val totalWait = 1.second

    override def processMessage(message: SQSMessage): Future[Unit] =
      Future.successful()
  }

  val worker = new TestWorker()

  it("processes messages") {
    worker.runSQSWorker()

    val testMessage = SQSMessage(
      subject = Some("subject"),
      messageType = "messageType",
      topic = "topic",
      body = "body",
      timestamp = "timestamp"
    )

    val testMessageJson = toJson(testMessage).get

    sqsClient.sendMessage(queueUrl, testMessageJson)

    eventually {
      verify(
        metricsSender,
        times(1)
      ).timeAndCount(
        contains("TestWorker_ProcessMessage"),
        any[() => Future[Unit]]()
      )
    }
    worker.cancelRun()
  }

  it("does not fail the TryBackoff run when unable to parse a message") {
    worker.runSQSWorker()
    sqsClient.sendMessage(queueUrl, "this is not valid Json")

    Thread.sleep(worker.totalWait.toMillis + 1000)

    verify(metricsSender, never()).incrementCount(anyString(), anyDouble())

    worker.cancelRun()
  }
}
