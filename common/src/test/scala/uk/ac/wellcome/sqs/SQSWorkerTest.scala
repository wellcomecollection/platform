package uk.ac.wellcome.sqs

import org.mockito.Matchers.{any, anyDouble, anyString, contains, matches}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.FunSpec
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar
import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{SQSConfig, SQSMessage}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.fixtures._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.JavaConversions._

import uk.ac.wellcome.utils.GlobalExecutionContext.context
import akka.actor.ActorSystem

class SQSWorkerTest
    extends FunSpec
    with MockitoSugar
    with Eventually
    with Akka
    with SQS {

  def withMockMetricSender[R](testWith: TestWith[MetricsSender, R]): R = {
    val metricsSender: MetricsSender = mock[MetricsSender]

    when(
      metricsSender
        .timeAndCount[Unit](anyString, any[() => Future[Unit]].apply)
    ).thenReturn(
      Future.successful(())
    )

    testWith(metricsSender)
  }

  def withSqsWorker[R](
    actors: ActorSystem,
    queueUrl: String,
    metrics: MetricsSender)(testWith: TestWith[SQSWorker, R]) = {
    val sqsReader = new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1))

    val testWorker =
      new SQSWorker(sqsReader, actors, metrics) {
        override def processMessage(message: SQSMessage) =
          Future.successful(())
      }

    try {
      testWith(testWorker)
    } finally {
      testWorker.stop()
    }
  }

  def ValidSqsMessage() = SQSMessage(
    subject = Some("subject"),
    messageType = "messageType",
    topic = "topic",
    body = "body",
    timestamp = "timestamp"
  )

  def withFixtures[R] =
    withActorSystem[R] and
      withLocalSqsQueue[R] and
      withMockMetricSender[R] _ and
      withSqsWorker[R] _

  it("processes messages") {
    withFixtures {
      case (_, queueUrl, metrics, worker) =>
        val json = toJson(ValidSqsMessage()).get

        sqsClient.sendMessage(queueUrl, json)

        eventually {
          verify(
            metrics,
            times(1)
          ).timeAndCount(
            matches(".*_ProcessMessage"),
            any()
          )
        }
    }
  }

  it("does report an error when a runtime error occurs") {
    withFixtures {
      case (_, queueUrl, metrics, worker) =>
        when(
          metrics.timeAndCount[Unit](
            anyString(),
            any[() => Future[Unit]].apply
          )
        ).thenThrow(new RuntimeException)

        val json = toJson(ValidSqsMessage()).get

        sqsClient.sendMessage(queueUrl, json)

        eventually {
          verify(metrics)
            .incrementCount(matches(".*_TerminalFailure"), anyDouble())
        }
    }
  }

  it("does not report an error when unable to parse a message") {
    withFixtures {
      case (_, queueUrl, metrics, worker) =>
        sqsClient.sendMessage(queueUrl, "this is not valid Json")

        eventually {
          verify(metrics, never())
            .incrementCount(matches(".*_TerminalFailure"), anyDouble())
        }
    }
  }
}
