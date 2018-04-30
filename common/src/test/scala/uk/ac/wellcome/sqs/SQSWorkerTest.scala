package uk.ac.wellcome.sqs

import org.mockito.Matchers.{any, anyDouble, anyString, matches}
import org.mockito.Mockito.{never, times, verify, when}

import org.scalatest.FunSpec
import org.scalatest.concurrent.Eventually
import org.scalatest.mockito.MockitoSugar

import uk.ac.wellcome.metrics.MetricsSender
import uk.ac.wellcome.models.aws.{SQSConfig, SQSMessage}
import uk.ac.wellcome.utils.JsonUtil._
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.fixtures

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.JavaConversions._

import akka.actor.ActorSystem
import uk.ac.wellcome.test.fixtures.SQS.Queue

class SQSWorkerTest
    extends FunSpec
    with MockitoSugar
    with Eventually
    with fixtures.Akka
    with fixtures.SQS
    with fixtures.MetricsSender {

  def withSqsWorker[R](
    actors: ActorSystem,
    queue: Queue,
    metrics: MetricsSender)(testWith: TestWith[SQSWorker, R]) = {
    val sqsReader = new SQSReader(sqsClient, SQSConfig(queue.url, 1.second, 1))

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
      withMockMetricSender[R] and
      withSqsWorker[R] _

  it("processes messages") {
    withFixtures {
      case (_, queue, metrics, worker) =>
        val json = toJson(ValidSqsMessage()).get

        sqsClient.sendMessage(queue.url, json)

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
      case (_, queue, metrics, worker) =>
        when(
          metrics.timeAndCount[Unit](
            anyString(),
            any[() => Future[Unit]].apply
          )
        ).thenThrow(new RuntimeException)

        val json = toJson(ValidSqsMessage()).get

        sqsClient.sendMessage(queue.url, json)

        eventually {
          verify(metrics)
            .incrementCount(matches(".*_TerminalFailure"), anyDouble())
        }
    }
  }

  it("does not report an error when unable to parse a message") {
    withFixtures {
      case (_, queue, metrics, worker) =>
        sqsClient.sendMessage(queue.url, "this is not valid Json")

        eventually {
          verify(metrics, never())
            .incrementCount(matches(".*_TerminalFailure"), anyDouble())
        }
    }
  }
}
