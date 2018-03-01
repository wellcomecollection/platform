package uk.ac.wellcome.sqs

import akka.actor.ActorSystem
import org.mockito.Matchers.{any, anyDouble, anyString, contains}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.fixture.FunSpec
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
import org.scalatest.Outcome

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

  override type FixtureParam = SQSWorker

  override def withFixture(test: OneArgTest): Outcome = {
    val actorSystem = ActorSystem()
    val sqsReader = new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1))

    val testWorker = new SQSWorker(sqsReader, actorSystem, metricsSender) {
      override lazy val poll = 1.second

      override def processMessage(message: SQSMessage): Future[Unit] =
        Future.successful(())
    }

    try {
      withFixture(test.toNoArgTest(testWorker))
    } finally {
      testWorker.stop()
      eventually { actorSystem.terminate() }
    }
  }

  sqsClient.setQueueAttributes(queueUrl, Map("VisibilityTimeout" -> "0"))

  it("processes messages") { worker =>
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
  }

  it("does not fail the TryBackoff run when unable to parse a message") { worker =>
    sqsClient.sendMessage(queueUrl, "this is not valid Json")

    Thread.sleep(worker.poll.toMillis + 1000)

    verify(metricsSender, never()).incrementCount(anyString(), anyDouble())
  }
}
