package uk.ac.wellcome.sqs

import org.mockito.Matchers.{any, anyDouble, anyString, contains, matches}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.fixture.FunSpec
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
import org.scalatest.Outcome

class SQSWorkerTest
    extends FunSpec
    with MockitoSugar
    with Eventually
    with SqsFixtures {

  trait MockMetricSender {
    val metricsSender: MetricsSender = mock[MetricsSender]

    when(
      metricsSender
        .timeAndCount[Unit](anyString, any[() => Future[Unit]].apply)
    ).thenReturn(
      Future.successful(())
    )
  }

  case class TestWorkerFixtures()
      extends TestWith[OneArgTest, Outcome]
      with MockMetricSender
      with AkkaFixtures {

    override def apply(test: OneArgTest) = {
      withActorSystem { actorSystem =>
        withLocalSqsQueue { queueUrl =>
          sqsClient.setQueueAttributes(
            queueUrl,
            Map("VisibilityTimeout" -> "0"))
          val sqsReader =
            new SQSReader(sqsClient, SQSConfig(queueUrl, 1.second, 1))

          val testWorker =
            new SQSWorker(sqsReader, actorSystem, metricsSender) {
              override def processMessage(message: SQSMessage) =
                Future.successful(())
            }

          try {
            withFixture(
              test.toNoArgTest(
                FixtureParam(metricsSender, testWorker, queueUrl)))
          } finally {
            testWorker.stop()
          }

        }
      }
    }

  }

  case class FixtureParam(metrics: MetricsSender,
                          worker: SQSWorker,
                          queueUrl: String)

  override def withFixture(test: OneArgTest) = TestWorkerFixtures()(test)

  def ValidSqsMessage() = SQSMessage(
    subject = Some("subject"),
    messageType = "messageType",
    topic = "topic",
    body = "body",
    timestamp = "timestamp"
  )

  it("processes messages") { fixtures =>
    val json = toJson(ValidSqsMessage()).get

    sqsClient.sendMessage(fixtures.queueUrl, json)

    eventually {
      verify(
        fixtures.metrics,
        times(1)
      ).timeAndCount(
        matches(".*_ProcessMessage"),
        any()
      )
    }
  }

  it("does report an error when a runtime error occurs") { fixtures =>
    when(
      fixtures.metrics.timeAndCount[Unit](
        anyString(),
        any[() => Future[Unit]].apply
      )
    ).thenThrow(new RuntimeException)

    val json = toJson(ValidSqsMessage()).get

    sqsClient.sendMessage(fixtures.queueUrl, json)

    eventually {
      verify(fixtures.metrics)
        .incrementCount(matches(".*_TerminalFailure"), anyDouble())
    }
  }

  it("does not report an error when unable to parse a message") { fixtures =>
    sqsClient.sendMessage(fixtures.queueUrl, "this is not valid Json")

    eventually {
      verify(fixtures.metrics, never())
        .incrementCount(matches(".*_TerminalFailure"), anyDouble())
    }
  }
}
